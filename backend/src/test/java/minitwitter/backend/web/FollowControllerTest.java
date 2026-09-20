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
class FollowControllerTest {

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
    @DisplayName("POST /follows crea la relación de seguimiento correctamente")
    void postFollows_relacionValida_ok() throws Exception {
        String json = "{\"followerId\":" + agomezId + ",\"followedId\":" + jperezId + "}";

        mockMvc.perform(post("/follows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .cookie(token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /follows retorna error 400 si algún usuario no existe")
    void postFollows_usuarioInexistente_error400() throws Exception {
        String json = "{\"followerId\":" + agomezId + ",\"followedId\":999999}";

        mockMvc.perform(post("/follows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .cookie(token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Usuario no encontrado: 999999")));
    }

    @Test
    @DisplayName("DELETE /follows elimina la relación de seguimiento correctamente")
    void deleteFollows_relacionExistente_ok() throws Exception {
        twitterService.follow(agomezId, jperezId);

        mockMvc.perform(delete("/follows")
                        .param("followerId", agomezId.toString())
                        .param("followedId", jperezId.toString())
                        .cookie(token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/follows/is-following")
                        .param("followerId", agomezId.toString())
                        .param("followedId", jperezId.toString())
                        .cookie(token))
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("GET /follows/is-following devuelve true si existe la relación")
    void getFollowsIsFollowing_relacionExistente_true() throws Exception {
        twitterService.follow(agomezId, jperezId);

        mockMvc.perform(get("/follows/is-following")
                        .param("followerId", agomezId.toString())
                        .param("followedId", jperezId.toString())
                        .cookie(token))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("GET /follows/followers devuelve quiénes siguen al usuario")
    void getFollowsFollowers_devuelveLosSeguidores_ok() throws Exception {
        twitterService.follow(agomezId, jperezId);

        mockMvc.perform(get("/follows/followers").param("userId", jperezId.toString()).cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username", is("agomez")));
    }

    @Test
    @DisplayName("GET /follows/following devuelve a quiénes sigue el usuario")
    void getFollowsFollowing_devuelveAQuienesSigue_ok() throws Exception {
        twitterService.follow(agomezId, jperezId);

        mockMvc.perform(get("/follows/following").param("userId", agomezId.toString()).cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username", is("jperez")));
    }

    @Test
    @DisplayName("GET /follows/following retorna error 400 si no se envía la cookie de sesión")
    void getFollowsFollowing_sinToken_error400() throws Exception {
        mockMvc.perform(get("/follows/following").param("userId", agomezId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("No autenticado")));
    }
}
