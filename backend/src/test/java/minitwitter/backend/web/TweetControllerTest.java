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
    private TwitterService twitterService;

    @Autowired
    private MockMvc mockMvc;

    private Integer agomezId;
    private Integer jperezId;
    private Cookie token;

    @BeforeEach
    void setUp() {
        emf.getSchemaManager().truncate();

        twitterService = new TwitterService(emf);
        twitterService.addUser("agomez", "password123");
        twitterService.addUser("jperez", "password456");

        agomezId = twitterService.getUserByUsername("agomez").orElseThrow().id();
        jperezId = twitterService.getUserByUsername("jperez").orElseThrow().id();

        token = new Cookie("token", twitterService.login("agomez", "password123"));
    }

    @Test
    @DisplayName("POST /tweets agrega un tweet válido correctamente")
    void postTweets_tweetValido_ok() throws Exception {
        String json = "{\"userId\":" + agomezId + ",\"content\":\"Mi primer tweet\"}";

        mockMvc.perform(post("/tweets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .cookie(token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /tweets devuelve solo los tweets originales del usuario")
    void getTweets_soloOriginalesDelUsuario_ok() throws Exception {
        twitterService.createTweet(agomezId, "Tweet de Ana");

        mockMvc.perform(get("/tweets").param("userId", agomezId.toString()).cookie(token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content", is("Tweet de Ana")))
                .andExpect(jsonPath("$[0].authorUsername", is("agomez")));
    }

    @Test
    @DisplayName("GET /tweets/timeline mezcla originales y retweets del usuario")
    void getTweetsTimeline_mezclaOriginalesYRetweets_ok() throws Exception {
        twitterService.createTweet(jperezId, "Tweet de Luis");
        Integer tweetId = twitterService.listTweetsForUserId(jperezId).get(0).id();
        twitterService.createRetweet(agomezId, tweetId);

        mockMvc.perform(get("/tweets/timeline").param("userId", agomezId.toString()).cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("DELETE /tweets/{id} borra el tweet correctamente")
    void deleteTweets_tweetExistente_ok() throws Exception {
        twitterService.createTweet(agomezId, "Tweet a borrar");
        Integer tweetId = twitterService.listTweetsForUserId(agomezId).get(0).id();

        mockMvc.perform(delete("/tweets/" + tweetId).cookie(token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tweets").param("userId", agomezId.toString()).cookie(token))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("DELETE /tweets/{id} retorna error 403 si el tweet es de otro usuario y no lo borra")
    void deleteTweets_tweetDeOtroUsuario_error403() throws Exception {
        twitterService.createTweet(jperezId, "Tweet de Luis");
        Integer tweetId = twitterService.listTweetsForUserId(jperezId).get(0).id();

        mockMvc.perform(delete("/tweets/" + tweetId).cookie(token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("No podés eliminar un tweet de otra persona")));

        mockMvc.perform(get("/tweets").param("userId", jperezId.toString()).cookie(token))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /tweets retorna error 401 si no se envía la cookie de sesión")
    void getTweets_sinToken_error401() throws Exception {
        mockMvc.perform(get("/tweets").param("userId", agomezId.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("No autenticado")));
    }

    @Test
    @DisplayName("GET /tweets/feed devuelve los tweets propios y los de los usuarios que sigue el usuario logueado")
    void getTweetsFeed_tweetsPropiosYDeLosSeguidos_ok() throws Exception {
        twitterService.createTweet(jperezId, "Tweet de Luis");
        twitterService.createTweet(agomezId, "Tweet propio de Ana");
        twitterService.follow(agomezId, jperezId);

        mockMvc.perform(get("/tweets/feed").cookie(token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].content", is("Tweet propio de Ana")))
                .andExpect(jsonPath("$[0].authorUsername", is("agomez")))
                .andExpect(jsonPath("$[1].content", is("Tweet de Luis")))
                .andExpect(jsonPath("$[1].authorUsername", is("jperez")));
    }

    @Test
    @DisplayName("GET /tweets/feed sigue mostrando un retweet de un seguido aunque el tweet original se haya dado de baja")
    void getTweetsFeed_retweetConOrigenBorrado_siguePresente() throws Exception {
        twitterService.addUser("mlopez_dev", "password789");
        Integer mlopezId = twitterService.getUserByUsername("mlopez_dev").orElseThrow().id();
        twitterService.createTweet(mlopezId, "Tweet original de Maria");
        Integer originalId = twitterService.listTweetsForUserId(mlopezId).get(0).id();
        twitterService.createRetweet(jperezId, originalId);
        twitterService.follow(agomezId, jperezId);
        twitterService.deleteTweet(mlopezId, originalId);

        mockMvc.perform(get("/tweets/feed").cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].authorUsername", is("jperez")))
                .andExpect(jsonPath("$[0].originalContent", is("Publicación no disponible")))
                .andExpect(jsonPath("$[0].originalDeleted", is(true)));
    }

    @Test
    @DisplayName("GET /tweets/feed retorna error 401 si no se envía la cookie de sesión")
    void getTweetsFeed_sinToken_error401() throws Exception {
        mockMvc.perform(get("/tweets/feed"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("No autenticado")));
    }
}
