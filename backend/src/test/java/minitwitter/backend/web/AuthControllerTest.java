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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = Main.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private EntityManagerFactory emf;
    private TwitterService servicioTwitter;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void prepararDatos() {
        emf.getSchemaManager().truncate();

        servicioTwitter = new TwitterService(emf);
        servicioTwitter.agregarUsuario("agomez", "password123");
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
    @DisplayName("POST /login retorna error 401 si la contraseña es incorrecta")
    void postLogin_contraseñaIncorrecta_error401() throws Exception {
        String json = "{\"username\":\"agomez\",\"password\":\"password_incorrecta\"}";

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Usuario o contraseña incorrectos")));
    }

    @Test
    @DisplayName("POST /logout expira la cookie token con los mismos atributos que el login")
    void postLogout_conSesion_expiraLaCookie() throws Exception {
        Cookie token = new Cookie("token", servicioTwitter.iniciarSesion("agomez", "password123"));

        mockMvc.perform(post("/logout").cookie(token))
                .andExpect(status().isOk())
                .andExpect(cookie().value("token", ""))
                .andExpect(cookie().maxAge("token", 0))
                .andExpect(cookie().path("token", "/"))
                .andExpect(cookie().httpOnly("token", true));
    }

    @Test
    @DisplayName("POST /logout funciona aunque no haya sesión (cookie ausente o vencida)")
    void postLogout_sinSesion_ok() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("token", 0));
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

    @Test
    @DisplayName("POST /registrar retorna error 400 si falta la contraseña o el username no es válido")
    void postRegistrar_datosInvalidos_error400() throws Exception {
        mockMvc.perform(post("/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"jperez\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("La contraseña es obligatoria.")));

        mockMvc.perform(post("/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"abc\",\"password\":\"password456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("El nombre de usuario debe tener entre 5 y 25 caracteres.")));
    }

    @Test
    @DisplayName("GET /me devuelve el usuario logueado según la cookie token")
    void getMe_conSesion_devuelveElUsuarioLogueado() throws Exception {
        Cookie token = new Cookie("token", servicioTwitter.iniciarSesion("agomez", "password123"));

        mockMvc.perform(get("/me").cookie(token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username", is("agomez")));
    }

    @Test
    @DisplayName("GET /me retorna error 401 si no se envía la cookie de sesión")
    void getMe_sinToken_error401() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("No autenticado")));
    }

    @Test
    @DisplayName("GET /me retorna error 401 si el usuario del token ya fue dado de baja")
    void getMe_usuarioBorrado_error401() throws Exception {
        Cookie token = new Cookie("token", servicioTwitter.iniciarSesion("agomez", "password123"));
        Integer id = servicioTwitter.obtenerUsuarioPorNombreUsuario("agomez").orElseThrow().id();
        servicioTwitter.eliminarUsuario(id);

        mockMvc.perform(get("/me").cookie(token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Sesión inválida")));
    }
}
