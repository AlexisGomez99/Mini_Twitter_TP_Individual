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
    private TwitterService twitterService;

    @Autowired
    private MockMvc mockMvc;

    private Integer agomezId;
    private Integer jperezId;
    private Integer jperezTweetId;
    private Cookie token;

    @BeforeEach
    void setUp() {
        emf.getSchemaManager().truncate();

        twitterService = new TwitterService(emf);
        twitterService.addUser("agomez", "password123");
        twitterService.addUser("jperez", "password456");

        agomezId = twitterService.getUserByUsername("agomez").orElseThrow().id();
        jperezId = twitterService.getUserByUsername("jperez").orElseThrow().id();

        twitterService.createTweet(jperezId, "Tweet de Luis");
        jperezTweetId = twitterService.listTweetsForUserId(jperezId).get(0).id();

        token = new Cookie("token", twitterService.login("agomez", "password123"));
    }

    @Test
    @DisplayName("POST /retweets agrega un retweet válido correctamente")
    void postRetweets_retweetValido_ok() throws Exception {
        String json = "{\"userId\":" + agomezId + ",\"originTweetId\":" + jperezTweetId + "}";

        mockMvc.perform(post("/retweets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .cookie(token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /retweets devuelve solo los retweets del usuario")
    void getRetweets_soloDelUsuario_ok() throws Exception {
        twitterService.createRetweet(agomezId, jperezTweetId);

        mockMvc.perform(get("/retweets").param("userId", agomezId.toString()).cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].authorUsername", is("agomez")))
                .andExpect(jsonPath("$[0].originalAuthorUsername", is("jperez")))
                .andExpect(jsonPath("$[0].originalDeleted", is(false)));
    }

    @Test
    @DisplayName("DELETE /retweets/{id} borra el retweet correctamente")
    void deleteRetweets_retweetExistente_ok() throws Exception {
        twitterService.createRetweet(agomezId, jperezTweetId);
        Integer retweetId = twitterService.listRetweetsForUserId(agomezId).get(0).id();

        mockMvc.perform(delete("/retweets/" + retweetId).cookie(token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/retweets").param("userId", agomezId.toString()).cookie(token))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /retweets retorna error 401 si no se envía la cookie de sesión")
    void getRetweets_sinToken_error401() throws Exception {
        mockMvc.perform(get("/retweets").param("userId", agomezId.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("No autenticado")));
    }
}
