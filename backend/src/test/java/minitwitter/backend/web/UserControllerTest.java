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
class UserControllerTest {

    @Autowired
    private EntityManagerFactory emf;
    private TwitterService twitterService;

    @Autowired
    private MockMvc mockMvc;

    private Cookie token;

    @BeforeEach
    void setUp() {
        emf.getSchemaManager().truncate();

        twitterService = new TwitterService(emf);
        twitterService.addUser("agomez", "password123");

        token = new Cookie("token", twitterService.login("agomez", "password123"));
    }

    @Test
    @DisplayName("POST /users agrega un usuario válido correctamente")
    void postUsers_usuarioValido_ok() throws Exception {
        String json = "{\"username\":\"jperez\",\"password\":\"password456\"}";

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /users devuelve el usuario si existe, buscando por username")
    void getUsers_usuarioExistente_ok() throws Exception {
        mockMvc.perform(get("/users").param("username", "agomez").cookie(token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username", is("agomez")));
    }

    @Test
    @DisplayName("GET /users retorna error 400 si el username no existe")
    void getUsers_usuarioInexistente_error400() throws Exception {
        mockMvc.perform(get("/users").param("username", "no_existe").cookie(token))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message", is("Usuario no encontrado: no_existe")));
    }

    @Test
    @DisplayName("GET /users sin parámetros devuelve todos los usuarios")
    void getUsers_sinParametros_devuelveTodosLosUsuarios() throws Exception {
        twitterService.addUser("jperez", "password456");

        mockMvc.perform(get("/users").cookie(token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /users/{id} devuelve el usuario si existe, buscando por id")
    void getUsersById_usuarioExistente_ok() throws Exception {
        Integer id = twitterService.getUserByUsername("agomez").orElseThrow().id();

        mockMvc.perform(get("/users/" + id).cookie(token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username", is("agomez")));
    }

    @Test
    @DisplayName("GET /users/{id} retorna error 400 si el id no existe")
    void getUsersById_usuarioInexistente_error400() throws Exception {
        mockMvc.perform(get("/users/999999").cookie(token))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message", is("Usuario no encontrado: 999999")));
    }

    @Test
    @DisplayName("GET /users/{id} retorna error 400 si no se envía la cookie de sesión")
    void getUsersById_sinToken_error400() throws Exception {
        Integer id = twitterService.getUserByUsername("agomez").orElseThrow().id();

        mockMvc.perform(get("/users/" + id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("No autenticado")));
    }

    @Test
    @DisplayName("DELETE /users/{id} borra el usuario correctamente")
    void deleteUsers_usuarioExistente_ok() throws Exception {
        Integer id = twitterService.getUserByUsername("agomez").orElseThrow().id();

        mockMvc.perform(delete("/users/" + id).cookie(token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/" + id).cookie(token))
                .andExpect(status().isBadRequest());
    }
}
