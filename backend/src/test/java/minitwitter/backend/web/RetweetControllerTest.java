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
class RetweetControllerTest {

    @Autowired
    private EntityManagerFactory emf;
    private TwitterService servicioTwitter;

    @Autowired
    private MockMvc mockMvc;

    private Integer idAgomez;
    private Integer idJperez;
    private Integer idTweetJperez;
    private Cookie token;

    @BeforeEach
    void prepararDatos() {
        emf.getSchemaManager().truncate();

        servicioTwitter = new TwitterService(emf);
        servicioTwitter.agregarUsuario("agomez", "password123");
        servicioTwitter.agregarUsuario("jperez", "password456");

        idAgomez = servicioTwitter.obtenerUsuarioPorNombreUsuario("agomez").orElseThrow().id();
        idJperez = servicioTwitter.obtenerUsuarioPorNombreUsuario("jperez").orElseThrow().id();

        servicioTwitter.crearTweet(idJperez, "Tweet de Luis");
        idTweetJperez = servicioTwitter.listarTweetsDelUsuario(idJperez, 0).content().get(0).id();

        token = new Cookie("token", servicioTwitter.iniciarSesion("agomez", "password123"));
    }

    @Test
    @DisplayName("POST /retweets agrega un retweet válido correctamente")
    void postRetweets_retweetValido_ok() throws Exception {
        String json = "{\"userId\":" + idAgomez + ",\"originTweetId\":" + idTweetJperez + "}";

        mockMvc.perform(post("/retweets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .cookie(token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /retweets devuelve solo los retweets del usuario")
    void getRetweets_soloDelUsuario_ok() throws Exception {
        servicioTwitter.crearRetweet(idAgomez, idTweetJperez);

        mockMvc.perform(get("/retweets").param("userId", idAgomez.toString()).cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].authorUsername", is("agomez")))
                .andExpect(jsonPath("$[0].originalAuthorUsername", is("jperez")))
                .andExpect(jsonPath("$[0].originalDeleted", is(false)));
    }

    @Test
    @DisplayName("DELETE /retweets/{id} borra el retweet correctamente")
    void deleteRetweets_retweetExistente_ok() throws Exception {
        servicioTwitter.crearRetweet(idAgomez, idTweetJperez);
        Integer idRetweet = servicioTwitter.listarRetweetsDelUsuario(idAgomez).get(0).id();

        mockMvc.perform(delete("/retweets/" + idRetweet).cookie(token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/retweets").param("userId", idAgomez.toString()).cookie(token))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /retweets retorna error 401 si no se envía la cookie de sesión")
    void getRetweets_sinToken_error401() throws Exception {
        mockMvc.perform(get("/retweets").param("userId", idAgomez.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("No autenticado")));
    }
}
