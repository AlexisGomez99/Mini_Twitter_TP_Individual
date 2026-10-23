package minitwitter.backend.web;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.Cookie;
import minitwitter.backend.service.TwitterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import minitwitter.backend.main.Main;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = Main.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TweetControllerTest {

    @Autowired
    private EntityManagerFactory emf;
    private TwitterService servicioTwitter;

    @Autowired
    private MockMvc mockMvc;

    private Integer idAgomez;
    private Integer idJperez;
    private Cookie token;

    @BeforeEach
    void prepararDatos() {
        emf.getSchemaManager().truncate();

        servicioTwitter = new TwitterService(emf);
        servicioTwitter.agregarUsuario("agomez", "password123");
        servicioTwitter.agregarUsuario("jperez", "password456");

        idAgomez = servicioTwitter.obtenerUsuarioPorNombreUsuario("agomez").orElseThrow().id();
        idJperez = servicioTwitter.obtenerUsuarioPorNombreUsuario("jperez").orElseThrow().id();

        token = new Cookie("token", servicioTwitter.iniciarSesion("agomez", "password123"));
    }

    @Test
    @DisplayName("POST /tweets agrega un tweet válido correctamente")
    void postTweets_tweetValido_ok() throws Exception {
        String json = "{\"userId\":" + idAgomez + ",\"content\":\"Mi primer tweet\"}";

        mockMvc.perform(post("/tweets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .cookie(token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /tweets devuelve solo los tweets originales del usuario")
    void getTweets_soloOriginalesDelUsuario_ok() throws Exception {
        servicioTwitter.crearTweet(idAgomez, "Tweet de Ana");

        mockMvc.perform(get("/tweets").param("userId", idAgomez.toString()).cookie(token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].content", is("Tweet de Ana")))
                .andExpect(jsonPath("$.content[0].authorUsername", is("agomez")));
    }

    @Test
    @DisplayName("DELETE /tweets/{id} borra el tweet correctamente")
    void deleteTweets_tweetExistente_ok() throws Exception {
        servicioTwitter.crearTweet(idAgomez, "Tweet a borrar");
        Integer idTweet = servicioTwitter.listarTweetsDelUsuario(idAgomez, 0).content().get(0).id();

        mockMvc.perform(delete("/tweets/" + idTweet).cookie(token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tweets").param("userId", idAgomez.toString()).cookie(token))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("DELETE /tweets/{id} retorna error 403 si el tweet es de otro usuario y no lo borra")
    void deleteTweets_tweetDeOtroUsuario_error403() throws Exception {
        servicioTwitter.crearTweet(idJperez, "Tweet de Luis");
        Integer idTweet = servicioTwitter.listarTweetsDelUsuario(idJperez, 0).content().get(0).id();

        mockMvc.perform(delete("/tweets/" + idTweet).cookie(token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("No podés eliminar un tweet de otra persona")));

        mockMvc.perform(get("/tweets").param("userId", idJperez.toString()).cookie(token))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("GET /tweets retorna error 401 si no se envía la cookie de sesión")
    void getTweets_sinToken_error401() throws Exception {
        mockMvc.perform(get("/tweets").param("userId", idAgomez.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("No autenticado")));
    }

    @Test
    @DisplayName("GET /tweets/feed devuelve los tweets propios y los de los usuarios que sigue el usuario logueado")
    void getTweetsFeed_tweetsPropiosYDeLosSeguidos_ok() throws Exception {
        servicioTwitter.crearTweet(idJperez, "Tweet de Luis");
        servicioTwitter.crearTweet(idAgomez, "Tweet propio de Ana");
        servicioTwitter.seguir(idAgomez, idJperez);

        mockMvc.perform(get("/tweets/feed").cookie(token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].content", is("Tweet propio de Ana")))
                .andExpect(jsonPath("$.content[0].authorUsername", is("agomez")))
                .andExpect(jsonPath("$.content[1].content", is("Tweet de Luis")))
                .andExpect(jsonPath("$.content[1].authorUsername", is("jperez")));
    }

    @Test
    @DisplayName("GET /tweets/feed devuelve el feed paginado de a 20 y el parámetro page elige la página")
    void getTweetsFeed_paginado_ok() throws Exception {
        for (int i = 1; i <= 21; i++) {
            servicioTwitter.crearTweet(idAgomez, "Tweet " + i);
        }

        mockMvc.perform(get("/tweets/feed").cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(20)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.totalElements", is(21)))
                .andExpect(jsonPath("$.hasNext", is(true)));

        mockMvc.perform(get("/tweets/feed").param("page", "1").cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page", is(1)))
                .andExpect(jsonPath("$.hasNext", is(false)));
    }

    @Test
    @DisplayName("GET /tweets retorna error 400 si page es negativa o no es un número")
    void getTweets_pageInvalida_error400() throws Exception {
        mockMvc.perform(get("/tweets").param("userId", idAgomez.toString()).param("page", "-1").cookie(token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("La página no puede ser negativa")));

        mockMvc.perform(get("/tweets").param("userId", idAgomez.toString()).param("page", "abc").cookie(token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Parámetros inválidos")));
    }

    @Test
    @DisplayName("GET /tweets/feed sigue mostrando un retweet de un seguido aunque el tweet original se haya dado de baja")
    void getTweetsFeed_retweetConOrigenBorrado_siguePresente() throws Exception {
        servicioTwitter.agregarUsuario("mlopez_dev", "password789");
        Integer idMlopez = servicioTwitter.obtenerUsuarioPorNombreUsuario("mlopez_dev").orElseThrow().id();
        servicioTwitter.crearTweet(idMlopez, "Tweet original de Maria");
        Integer idOriginal = servicioTwitter.listarTweetsDelUsuario(idMlopez, 0).content().get(0).id();
        servicioTwitter.crearRetweet(idJperez, idOriginal);
        servicioTwitter.seguir(idAgomez, idJperez);
        servicioTwitter.eliminarTweet(idMlopez, idOriginal);

        mockMvc.perform(get("/tweets/feed").cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].authorUsername", is("jperez")))
                .andExpect(jsonPath("$.content[0].originalContent", is("Publicación no disponible")))
                .andExpect(jsonPath("$.content[0].originalDeleted", is(true)));
    }

    @Test
    @DisplayName("GET /tweets/feed retorna error 401 si no se envía la cookie de sesión")
    void getTweetsFeed_sinToken_error401() throws Exception {
        mockMvc.perform(get("/tweets/feed"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("No autenticado")));
    }
}
