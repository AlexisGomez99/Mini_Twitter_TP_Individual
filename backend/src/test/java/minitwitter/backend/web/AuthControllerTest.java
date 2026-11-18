package minitwitter.backend.web;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.Cookie;
import minitwitter.backend.main.Main;
import minitwitter.backend.service.TwitterService;
import minitwitter.backend.util.DatosDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static minitwitter.backend.service.TwitterService.ERROR_CREDENCIALES_INCORRECTAS;
import static minitwitter.backend.service.TwitterService.ERROR_TOKEN_INVALIDO;
import static minitwitter.backend.service.TwitterService.ERROR_USUARIO_YA_EXISTE;
import static minitwitter.backend.util.DatosDePrueba.AGOMEZ;
import static minitwitter.backend.util.DatosDePrueba.CONTRASENIA_AGOMEZ;
import static minitwitter.backend.util.RespuestasHttp.estado;
import static minitwitter.backend.util.RespuestasHttp.valorJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(classes = Main.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TwitterService servicioTwitter;

    @BeforeEach
    void antesDeCadaTest() {
        emf.getSchemaManager().truncate();
        DatosDePrueba.cargar(emf);
    }

    private static String credenciales(String nombreUsuario, String contrasenia) {
        return "{\"username\":\"" + nombreUsuario + "\",\"password\":\"" + contrasenia + "\"}";
    }

    private MvcResult iniciarSesionPorHttp(String nombreUsuario, String contrasenia) throws Exception {
        return mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(nombreUsuario, contrasenia)))
                .andReturn();
    }

    private Cookie cookieDeSesion() {
        return new Cookie(AuthController.NOMBRE_COOKIE, servicioTwitter.iniciarSesion(AGOMEZ, CONTRASENIA_AGOMEZ));
    }

    // ---------- POST /login ----------

    @Test
    @DisplayName("POST /login responde 200 y entrega la cookie de sesión cuando las credenciales son válidas")
    void login_entregaLaCookieDeSesionCuandoLasCredencialesSonValidas() throws Exception {
        // Setup: credenciales de un usuario existente
        // (AGOMEZ y su contraseña)

        // Ejercitación: iniciar sesión por HTTP
        MvcResult resultado = iniciarSesionPorHttp(AGOMEZ, CONTRASENIA_AGOMEZ);

        // Verificación: responde 200 e incluye la cookie con el token
        assertEquals(200, estado(resultado), "El login válido debe responder 200");
        assertNotNull(resultado.getResponse().getCookie(AuthController.NOMBRE_COOKIE),
                "La respuesta debe incluir la cookie de sesión");
    }

    @Test
    @DisplayName("POST /login entrega una cookie HttpOnly con la duración configurada")
    void login_entregaUnaCookieHttpOnlyConLaDuracionConfigurada() throws Exception {
        // Setup: credenciales de un usuario existente
        // (AGOMEZ y su contraseña)

        // Ejercitación: iniciar sesión por HTTP
        MvcResult resultado = iniciarSesionPorHttp(AGOMEZ, CONTRASENIA_AGOMEZ);

        // Verificación: la cookie no es legible desde JavaScript y vence en el tiempo configurado
        Cookie cookie = resultado.getResponse().getCookie(AuthController.NOMBRE_COOKIE);
        assertTrue(cookie.isHttpOnly(), "La cookie debe ser HttpOnly");
        assertEquals(AuthController.DURACION_COOKIE_SEGUNDOS, cookie.getMaxAge(),
                "La cookie debe durar lo configurado");
    }

    @Test
    @DisplayName("POST /login responde 401 cuando la contraseña es incorrecta")
    void login_responde401CuandoLaContraseniaEsIncorrecta() throws Exception {
        // Setup: contraseña equivocada para un usuario existente
        String contraseniaIncorrecta = "otra_clave";

        // Ejercitación: iniciar sesión por HTTP
        MvcResult resultado = iniciarSesionPorHttp(AGOMEZ, contraseniaIncorrecta);

        // Verificación: 401 con el mensaje de credenciales incorrectas
        assertEquals(401, estado(resultado), "Una contraseña incorrecta debe responder 401");
        assertEquals(ERROR_CREDENCIALES_INCORRECTAS, valorJson(resultado, "$.message"),
                "Debe informar que las credenciales son incorrectas");
    }

    @Test
    @DisplayName("POST /login responde 401 cuando el usuario no existe")
    void login_responde401CuandoElUsuarioNoExiste() throws Exception {
        // Setup: un nombre que no está registrado
        String nombreInexistente = "no_existe";

        // Ejercitación: iniciar sesión por HTTP
        MvcResult resultado = iniciarSesionPorHttp(nombreInexistente, CONTRASENIA_AGOMEZ);

        // Verificación: 401 con el mensaje de credenciales incorrectas
        assertEquals(401, estado(resultado), "Un usuario inexistente debe responder 401");
        assertEquals(ERROR_CREDENCIALES_INCORRECTAS, valorJson(resultado, "$.message"),
                "Debe informar que las credenciales son incorrectas");
    }

    @Test
    @DisplayName("POST /login responde 400 cuando no se envía el cuerpo")
    void login_responde400CuandoNoSeEnviaElCuerpo() throws Exception {
        // Setup: un pedido sin cuerpo
        // (no se necesita setup adicional)

        // Ejercitación: enviar el pedido vacío
        MvcResult resultado = mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)).andReturn();

        // Verificación: 400 por parámetros inválidos
        assertEquals(400, estado(resultado), "Un login sin cuerpo debe responder 400");
        assertEquals(GlobalExceptionHandler.ERROR_PARAMETROS_INVALIDOS, valorJson(resultado, "$.message"),
                "Debe informar que los parámetros son inválidos");
    }

    // ---------- POST /logout ----------

    @Test
    @DisplayName("POST /logout expira la cookie de sesión cuando hay sesión iniciada")
    void logout_expiraLaCookieCuandoHaySesion() throws Exception {
        // Setup: un usuario con la cookie de sesión
        Cookie cookie = cookieDeSesion();

        // Ejercitación: cerrar sesión
        MvcResult resultado = mockMvc.perform(post("/logout").cookie(cookie)).andReturn();

        // Verificación: responde 200 y devuelve la cookie vacía con duración cero
        Cookie cookieExpirada = resultado.getResponse().getCookie(AuthController.NOMBRE_COOKIE);
        assertEquals(200, estado(resultado), "El logout debe responder 200");
        assertEquals("", cookieExpirada.getValue(), "La cookie devuelta debe estar vacía");
        assertEquals(0, cookieExpirada.getMaxAge(), "La cookie devuelta debe vencer de inmediato");
    }

    @Test
    @DisplayName("POST /logout responde 200 aunque no haya sesión")
    void logout_responde200CuandoNoHaySesion() throws Exception {
        // Setup: un pedido sin cookie
        // (no se necesita setup adicional)

        // Ejercitación: cerrar sesión
        MvcResult resultado = mockMvc.perform(post("/logout")).andReturn();

        // Verificación: responde 200 igual
        assertEquals(200, estado(resultado), "Cerrar sesión sin sesión previa no debe fallar");
    }

    // ---------- POST /registrar ----------

    @Test
    @DisplayName("POST /registrar crea el usuario sin necesitar sesión")
    void registrar_creaElUsuarioSinNecesitarSesion() throws Exception {
        // Setup: un usuario nuevo y ninguna cookie
        String nombreNuevo = "lgomez";
        String contraseniaNueva = "clave123";

        // Ejercitación: registrarlo por HTTP
        MvcResult resultado = mockMvc.perform(post("/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(nombreNuevo, contraseniaNueva)))
                .andReturn();

        // Verificación: responde 200 y el usuario ya puede iniciar sesión
        assertEquals(200, estado(resultado), "El registro debe responder 200");
        assertNotNull(servicioTwitter.iniciarSesion(nombreNuevo, contraseniaNueva),
                "El usuario registrado debe poder iniciar sesión");
    }

    @Test
    @DisplayName("POST /registrar responde 400 cuando el nombre de usuario ya existe")
    void registrar_responde400CuandoElNombreYaExiste() throws Exception {
        // Setup: un nombre que ya pertenece a un usuario
        // (AGOMEZ)

        // Ejercitación: intentar registrarlo de nuevo
        MvcResult resultado = mockMvc.perform(post("/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(AGOMEZ, "otra_clave")))
                .andReturn();

        // Verificación: 400 con el mensaje de nombre existente
        assertEquals(400, estado(resultado), "Un nombre repetido debe responder 400");
        assertEquals(ERROR_USUARIO_YA_EXISTE, valorJson(resultado, "$.message"),
                "Debe informar que el nombre de usuario ya existe");
    }

    // ---------- GET /me ----------

    @Test
    @DisplayName("GET /me devuelve el usuario logueado según la cookie de sesión")
    void me_devuelveElUsuarioLogueado() throws Exception {
        // Setup: un usuario con la cookie de sesión
        Cookie cookie = cookieDeSesion();

        // Ejercitación: consultar quién está logueado
        MvcResult resultado = mockMvc.perform(get("/me").cookie(cookie)).andReturn();

        // Verificación: responde 200 con los datos del usuario
        assertEquals(200, estado(resultado), "Con sesión válida debe responder 200");
        assertEquals(AGOMEZ, valorJson(resultado, "$.username"), "Debe devolver al usuario de la sesión");
    }

    @Test
    @DisplayName("GET /me responde 401 cuando no se envía la cookie")
    void me_responde401CuandoNoSeEnviaLaCookie() throws Exception {
        // Setup: un pedido sin cookie
        // (no se necesita setup adicional)

        // Ejercitación: consultar quién está logueado
        MvcResult resultado = mockMvc.perform(get("/me")).andReturn();

        // Verificación: 401 con el mensaje de no autenticado
        assertEquals(401, estado(resultado), "Sin cookie debe responder 401");
        assertEquals(GlobalExceptionHandler.ERROR_NO_AUTENTICADO, valorJson(resultado, "$.message"),
                "Debe informar que no está autenticado");
    }

    @Test
    @DisplayName("GET /me responde 401 cuando el token es inválido")
    void me_responde401CuandoElTokenEsInvalido() throws Exception {
        // Setup: una cookie con un texto que no es un token
        Cookie cookieInvalida = new Cookie(AuthController.NOMBRE_COOKIE, "esto-no-es-un-token");

        // Ejercitación: consultar quién está logueado
        MvcResult resultado = mockMvc.perform(get("/me").cookie(cookieInvalida)).andReturn();

        // Verificación: 401 con el mensaje de token inválido
        assertEquals(401, estado(resultado), "Un token inválido debe responder 401");
        assertEquals(ERROR_TOKEN_INVALIDO, valorJson(resultado, "$.message"), "Debe informar que el token es inválido");
    }

    @Test
    @DisplayName("GET /me responde 401 cuando el usuario de la sesión fue borrado")
    void me_responde401CuandoElUsuarioFueBorrado() throws Exception {
        // Setup: un usuario con sesión iniciada que después es eliminado
        Cookie cookie = cookieDeSesion();
        servicioTwitter.eliminarUsuario(DatosDePrueba.idUsuario(emf, AGOMEZ));

        // Ejercitación: consultar quién está logueado
        MvcResult resultado = mockMvc.perform(get("/me").cookie(cookie)).andReturn();

        // Verificación: 401 con el mensaje de sesión inválida
        assertEquals(401, estado(resultado), "Un usuario borrado no debe conservar la sesión");
        assertEquals(AuthController.ERROR_SESION_INVALIDA, valorJson(resultado, "$.message"),
                "Debe informar que la sesión es inválida");
    }
}
