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

import static minitwitter.backend.service.TwitterService.ERROR_USUARIO_NO_ENCONTRADO;
import static minitwitter.backend.util.DatosDePrueba.AGOMEZ;
import static minitwitter.backend.util.DatosDePrueba.CONTRASENIA_AGOMEZ;
import static minitwitter.backend.util.DatosDePrueba.ID_INEXISTENTE;
import static minitwitter.backend.util.DatosDePrueba.JPEREZ;
import static minitwitter.backend.util.DatosDePrueba.MLOPEZ;
import static minitwitter.backend.util.RespuestasHttp.cantidadJson;
import static minitwitter.backend.util.RespuestasHttp.estado;
import static minitwitter.backend.util.RespuestasHttp.valorJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(classes = Main.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TwitterService servicioTwitter;

    private Integer idJperez;
    private Cookie cookie;

    @BeforeEach
    void antesDeCadaTest() {
        emf.getSchemaManager().truncate();
        DatosDePrueba.cargar(emf);
        idJperez = DatosDePrueba.idUsuario(emf, JPEREZ);
        cookie = new Cookie(AuthController.NOMBRE_COOKIE, servicioTwitter.iniciarSesion(AGOMEZ, CONTRASENIA_AGOMEZ));
    }

    @Test
    @DisplayName("POST /users crea el usuario sin necesitar sesión")
    void agregarUsuario_creaElUsuarioSinNecesitarSesion() throws Exception {
        // Setup: un usuario nuevo y ninguna cookie
        String json = "{\"username\":\"lgomez\",\"password\":\"clave123\"}";

        // Ejercitación: crearlo por HTTP
        MvcResult resultado = mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn();

        // Verificación: responde 200 y el usuario ya existe
        assertEquals(200, estado(resultado), "Crear un usuario debe responder 200 aunque no haya sesión");
        assertTrue(servicioTwitter.obtenerUsuarioPorNombreUsuario("lgomez").isPresent(),
                "El usuario creado debe poder consultarse");
    }

    @Test
    @DisplayName("GET /users devuelve todos los usuarios")
    void listarUsuarios_devuelveTodosLosUsuarios() throws Exception {
        // Setup: el escenario inicial tiene tres usuarios
        // (escenario inicial)

        // Ejercitación: listar los usuarios
        MvcResult resultado = mockMvc.perform(get("/users").cookie(cookie)).andReturn();

        // Verificación: responde 200 con los tres usuarios
        assertEquals(200, estado(resultado), "Debe responder 200");
        assertEquals(3, cantidadJson(resultado, "$"), "Debe devolver los tres usuarios del escenario");
    }

    @Test
    @DisplayName("GET /users?username devuelve el usuario cuando existe")
    void obtenerUsuarioPorNombre_devuelveElUsuarioCuandoExiste() throws Exception {
        // Setup: un usuario existente
        // (JPEREZ)

        // Ejercitación: buscarlo por nombre
        MvcResult resultado = mockMvc.perform(get("/users").param("username", JPEREZ).cookie(cookie)).andReturn();

        // Verificación: responde 200 con ese usuario
        assertEquals(200, estado(resultado), "Debe responder 200");
        assertEquals(JPEREZ, valorJson(resultado, "$.username"), "Debe devolver al usuario buscado");
    }

    @Test
    @DisplayName("GET /users?username responde 400 cuando el usuario no existe")
    void obtenerUsuarioPorNombre_responde400CuandoNoExiste() throws Exception {
        // Setup: un nombre que no está registrado
        String nombreInexistente = "no_existe";

        // Ejercitación: buscarlo por nombre
        MvcResult resultado = mockMvc.perform(get("/users").param("username", nombreInexistente).cookie(cookie))
                .andReturn();

        // Verificación: 400 con el mensaje de usuario no encontrado
        assertEquals(400, estado(resultado), "Un usuario inexistente debe responder 400");
        assertEquals(ERROR_USUARIO_NO_ENCONTRADO + nombreInexistente, valorJson(resultado, "$.message"),
                "Debe informar que el usuario no fue encontrado");
    }

    @Test
    @DisplayName("GET /users/{id} devuelve el usuario cuando existe")
    void obtenerUsuarioPorId_devuelveElUsuarioCuandoExiste() throws Exception {
        // Setup: el id de un usuario existente
        // (idJperez, obtenido en el antesDeCadaTest)

        // Ejercitación: buscarlo por id
        MvcResult resultado = mockMvc.perform(get("/users/" + idJperez).cookie(cookie)).andReturn();

        // Verificación: responde 200 con ese usuario
        assertEquals(200, estado(resultado), "Debe responder 200");
        assertEquals(JPEREZ, valorJson(resultado, "$.username"), "Debe devolver al usuario del id buscado");
    }

    @Test
    @DisplayName("GET /users/{id} responde 400 cuando el usuario no existe")
    void obtenerUsuarioPorId_responde400CuandoNoExiste() throws Exception {
        // Setup: un id que no corresponde a ningún usuario
        // (ID_INEXISTENTE)

        // Ejercitación: buscarlo por id
        MvcResult resultado = mockMvc.perform(get("/users/" + ID_INEXISTENTE).cookie(cookie)).andReturn();

        // Verificación: 400 con el mensaje de usuario no encontrado
        assertEquals(400, estado(resultado), "Un id inexistente debe responder 400");
        assertEquals(ERROR_USUARIO_NO_ENCONTRADO + ID_INEXISTENTE, valorJson(resultado, "$.message"),
                "Debe informar que el usuario no fue encontrado");
    }

    @Test
    @DisplayName("GET /users/{id} responde 401 cuando no se envía la cookie")
    void obtenerUsuarioPorId_responde401CuandoNoSeEnviaLaCookie() throws Exception {
        // Setup: un pedido sin cookie
        // (no se necesita setup adicional)

        // Ejercitación: buscar un usuario sin sesión
        MvcResult resultado = mockMvc.perform(get("/users/" + idJperez)).andReturn();

        // Verificación: 401 con el mensaje de no autenticado
        assertEquals(401, estado(resultado), "Sin cookie debe responder 401");
        assertEquals(GlobalExceptionHandler.ERROR_NO_AUTENTICADO, valorJson(resultado, "$.message"),
                "Debe informar que no está autenticado");
    }

    @Test
    @DisplayName("DELETE /users/{id} responde 200 y el usuario deja de poder consultarse")
    void eliminarUsuario_responde200YElUsuarioDejaDePoderConsultarse() throws Exception {
        // Setup: agomez, con sesión iniciada, elimina su propia cuenta
        Integer idAgomez = DatosDePrueba.idUsuario(emf, AGOMEZ);

        // Ejercitación: eliminarla por HTTP
        MvcResult resultado = mockMvc.perform(delete("/users/" + idAgomez).cookie(cookie)).andReturn();

        // Verificación: responde 200 y ya no se la encuentra
        assertEquals(200, estado(resultado), "Eliminar la propia cuenta debe responder 200");
        assertTrue(servicioTwitter.obtenerUsuarioPorId(idAgomez).isEmpty(),
                "El usuario eliminado no debe poder consultarse");
    }

    @Test
    @DisplayName("DELETE /users/{id} responde 403 cuando la cuenta es de otra persona")
    void eliminarUsuario_responde403CuandoLaCuentaEsDeOtraPersona() throws Exception {
        // Setup: la cuenta de mlopez, que intenta borrar agomez (la sesión)
        Integer idMlopez = DatosDePrueba.idUsuario(emf, MLOPEZ);

        // Ejercitación: intentar eliminarla por HTTP
        MvcResult resultado = mockMvc.perform(delete("/users/" + idMlopez).cookie(cookie)).andReturn();

        // Verificación: 403 con el mensaje de cuenta ajena y la cuenta sigue existiendo
        assertEquals(403, estado(resultado), "Eliminar una cuenta ajena debe responder 403");
        assertEquals(UserController.ERROR_ELIMINAR_CUENTA_AJENA, valorJson(resultado, "$.message"),
                "Debe informar que no se puede eliminar la cuenta de otra persona");
        assertTrue(servicioTwitter.obtenerUsuarioPorId(idMlopez).isPresent(),
                "La cuenta ajena no debe eliminarse");
    }

    @Test
    @DisplayName("DELETE /users/{id} responde 401 cuando no se envía la cookie")
    void eliminarUsuario_responde401CuandoNoSeEnviaLaCookie() throws Exception {
        // Setup: un pedido sin cookie
        // (no se necesita setup adicional)

        // Ejercitación: eliminar sin sesión
        MvcResult resultado = mockMvc.perform(delete("/users/" + idJperez)).andReturn();

        // Verificación: responde 401
        assertEquals(401, estado(resultado), "Sin cookie debe responder 401");
    }
}
