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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = Main.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private EntityManagerFactory emf;
    private TwitterService twitterService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        emf.getSchemaManager().truncate();

        twitterService = new TwitterService(emf);
        twitterService.addUser("agomez", "password123");
    }

    @Test
    @DisplayName("POST /login devuelve la cookie token si las credenciales son válidas")
    void postLogin_credencialesValidas_ok() throws Exception {
        String json = "{\"username\":\"agomez\",\"password\":\"password123\"}";

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("token"));
    }

    @Test
    @DisplayName("POST /login retorna error 400 si la contraseña es incorrecta")
    void postLogin_contraseñaIncorrecta_error400() throws Exception {
        String json = "{\"username\":\"agomez\",\"password\":\"password_incorrecta\"}";

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Usuario o contraseña incorrectos")));
    }

    @Test
    @DisplayName("POST /login retorna error 400 si el usuario no existe")
    void postLogin_usuarioInexistente_error400() throws Exception {
        String json = "{\"username\":\"no_existe\",\"password\":\"cualquier_clave\"}";

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Usuario o contraseña incorrectos")));
    }

    @Test
    @DisplayName("POST /registrar crea un usuario nuevo correctamente")
    void postRegistrar_usuarioNuevo_ok() throws Exception {
        String json = "{\"username\":\"jperez\",\"password\":\"password456\"}";

        mockMvc.perform(post("/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /registrar retorna error 400 si el username ya existe")
    void postRegistrar_usuarioExistente_error400() throws Exception {
        String json = "{\"username\":\"agomez\",\"password\":\"otra_clave\"}";

        mockMvc.perform(post("/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("El nombre de usuario ya existe")));
    }
}
