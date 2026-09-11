package minitwitter.backend.web;

import jakarta.persistence.EntityManagerFactory;
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

    private Long agomezId;
    private Long jperezId;
    private Long jperezTweetId;

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
    }

    @Test
    @DisplayName("POST /retweets agrega un retweet válido correctamente")
    void postRetweets_retweetValido_ok() throws Exception {
        String json = "{\"userId\":" + agomezId + ",\"originTweetId\":" + jperezTweetId + "}";

        mockMvc.perform(post("/retweets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /retweets retorna error 400 si el tweet de origen no existe")
    void postRetweets_tweetInexistente_error400() throws Exception {
        String json = "{\"userId\":" + agomezId + ",\"originTweetId\":999999}";

        mockMvc.perform(post("/retweets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Tweet no encontrado: 999999")));
    }

    @Test
    @DisplayName("GET /retweets devuelve solo los retweets del usuario")
    void getRetweets_soloDelUsuario_ok() throws Exception {
        twitterService.createRetweet(agomezId, jperezTweetId);

        mockMvc.perform(get("/retweets").param("userId", agomezId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].authorUsername", is("agomez")))
                .andExpect(jsonPath("$[0].originalAuthorUsername", is("jperez")));
    }

    @Test
    @DisplayName("GET /retweets muestra el retweet como no disponible si se borró el tweet de origen")
    void getRetweets_origenBorrado_muestraNoDisponible() throws Exception {
        twitterService.createRetweet(agomezId, jperezTweetId);

        mockMvc.perform(delete("/tweets/" + jperezTweetId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/retweets").param("userId", agomezId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].originalContent", is("Publicación no disponible")))
                .andExpect(jsonPath("$[0].originalAuthorUsername", is("jperez")));
    }

    @Test
    @DisplayName("DELETE /retweets/{id} borra el retweet correctamente")
    void deleteRetweets_retweetExistente_ok() throws Exception {
        twitterService.createRetweet(agomezId, jperezTweetId);
        Long retweetId = twitterService.listRetweetsForUserId(agomezId).get(0).id();

        mockMvc.perform(delete("/retweets/" + retweetId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/retweets").param("userId", agomezId.toString()))
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
