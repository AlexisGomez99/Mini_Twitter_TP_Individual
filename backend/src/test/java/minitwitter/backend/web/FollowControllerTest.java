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
import static minitwitter.backend.util.DatosDePrueba.CONTRASENIA_MLOPEZ;
import static minitwitter.backend.util.DatosDePrueba.ID_INEXISTENTE;
import static minitwitter.backend.util.DatosDePrueba.JPEREZ;
import static minitwitter.backend.util.DatosDePrueba.MLOPEZ;
import static minitwitter.backend.util.RespuestasHttp.cantidadJson;
import static minitwitter.backend.util.RespuestasHttp.estado;
import static minitwitter.backend.util.RespuestasHttp.valorJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(classes = Main.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FollowControllerTest {

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TwitterService servicioTwitter;

    private Integer idAgomez;
    private Integer idJperez;
    private Integer idMlopez;
    private Cookie cookie;
    private Cookie cookieDeMlopez;

    @BeforeEach
    void antesDeCadaTest() {
        emf.getSchemaManager().truncate();
        DatosDePrueba.cargar(emf);
        idAgomez = DatosDePrueba.idUsuario(emf, AGOMEZ);
        idJperez = DatosDePrueba.idUsuario(emf, JPEREZ);
        idMlopez = DatosDePrueba.idUsuario(emf, MLOPEZ);
        cookie = new Cookie(AuthController.NOMBRE_COOKIE, servicioTwitter.iniciarSesion(AGOMEZ, CONTRASENIA_AGOMEZ));
        cookieDeMlopez = new Cookie(AuthController.NOMBRE_COOKIE, servicioTwitter.iniciarSesion(MLOPEZ, CONTRASENIA_MLOPEZ));
    }

    private MvcResult seguirPorHttp(String cuerpoJson, Cookie cookieDeSesion) throws Exception {
        var pedido = post("/follows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoJson);
        if (cookieDeSesion != null) {
            pedido.cookie(cookieDeSesion);
        }
        return mockMvc.perform(pedido).andReturn();
    }

    // Cuerpo actual: solo a quién seguir (el seguidor sale de la cookie).
    private static String cuerpoDeSeguir(Integer idSeguido) {
        return "{\"followedId\":" + idSeguido + "}";
    }

    private MvcResult consultarSiSigue(Integer idSeguidor, Integer idSeguido) throws Exception {
        return mockMvc.perform(get("/follows/is-following")
                        .param("followerId", idSeguidor.toString())
                        .param("followedId", idSeguido.toString())
                        .cookie(cookie))
                .andReturn();
    }

    @Test
    @DisplayName("POST /follows responde 200 y crea la relación de seguimiento")
    void seguir_responde200YCreaLaRelacion() throws Exception {
        // Setup: mlopez, con sesión iniciada, todavía no sigue a agomez
        // (escenario inicial)

        // Ejercitación: seguir por HTTP
        MvcResult resultado = seguirPorHttp(cuerpoDeSeguir(idAgomez), cookieDeMlopez);

        // Verificación: responde 200 y la relación existe
        assertEquals(200, estado(resultado), "Seguir a alguien debe responder 200");
        assertTrue(servicioTwitter.estaSiguiendo(idMlopez, idAgomez), "La relación debe quedar creada");
    }

    @Test
    @DisplayName("POST /follows sigue en nombre de la sesión aunque el cuerpo traiga el followerId de otro usuario")
    void seguir_siguePorLaSesionCuandoElCuerpoTraeOtroFollowerId() throws Exception {
        // Setup: sesión de mlopez y un cuerpo que intenta hacer seguir a jperez
        String cuerpo = "{\"followerId\":" + idJperez + ",\"followedId\":" + idAgomez + "}";

        // Ejercitación: seguir por HTTP
        MvcResult resultado = seguirPorHttp(cuerpo, cookieDeMlopez);

        // Verificación: la relación la crea mlopez y jperez no empieza a seguir a nadie
        assertEquals(200, estado(resultado), "Debe responder 200: el followerId del cuerpo se ignora");
        assertTrue(servicioTwitter.estaSiguiendo(idMlopez, idAgomez),
                "La relación debe crearse a nombre del usuario de la sesión");
        assertFalse(servicioTwitter.estaSiguiendo(idJperez, idAgomez),
                "El usuario del followerId del cuerpo no debe seguir a nadie");
    }

    @Test
    @DisplayName("POST /follows responde 400 cuando uno de los usuarios no existe")
    void seguir_responde400CuandoUnUsuarioNoExiste() throws Exception {
        // Setup: un seguido inexistente
        // (ID_INEXISTENTE)

        // Ejercitación: seguir por HTTP
        MvcResult resultado = seguirPorHttp(cuerpoDeSeguir(ID_INEXISTENTE), cookie);

        // Verificación: 400 con el mensaje de usuario no encontrado
        assertEquals(400, estado(resultado), "Un usuario inexistente debe responder 400");
        assertEquals(ERROR_USUARIO_NO_ENCONTRADO + ID_INEXISTENTE, valorJson(resultado, "$.message"),
                "Debe informar que el usuario no fue encontrado");
    }

    @Test
    @DisplayName("POST /follows responde 401 cuando no se envía la cookie")
    void seguir_responde401CuandoNoSeEnviaLaCookie() throws Exception {
        // Setup: un pedido sin cookie
        // (no se necesita setup adicional)

        // Ejercitación: seguir sin sesión
        MvcResult resultado = seguirPorHttp(cuerpoDeSeguir(idAgomez), null);

        // Verificación: responde 401
        assertEquals(401, estado(resultado), "Sin cookie debe responder 401");
    }

    @Test
    @DisplayName("DELETE /follows responde 200 y elimina la relación de seguimiento")
    void dejarDeSeguir_responde200YEliminaLaRelacion() throws Exception {
        // Setup: agomez sigue a jperez en el escenario inicial
        // (escenario inicial)

        // Ejercitación: dejar de seguir por HTTP
        MvcResult resultado = mockMvc.perform(delete("/follows")
                        .param("followedId", idJperez.toString())
                        .cookie(cookie))
                .andReturn();

        // Verificación: responde 200 y la relación ya no existe
        assertEquals(200, estado(resultado), "Dejar de seguir debe responder 200");
        assertFalse(servicioTwitter.estaSiguiendo(idAgomez, idJperez), "La relación debe quedar eliminada");
    }

    @Test
    @DisplayName("DELETE /follows actúa sobre la sesión aunque la URL traiga el followerId de otro usuario")
    void dejarDeSeguir_actuaSobreLaSesionCuandoLaUrlTraeOtroFollowerId() throws Exception {
        // Setup: agomez sigue a jperez; la sesión es de mlopez, que intenta romper esa relación
        // (escenario inicial)

        // Ejercitación: dejar de seguir por HTTP indicando como seguidor a agomez
        MvcResult resultado = mockMvc.perform(delete("/follows")
                        .param("followerId", idAgomez.toString())
                        .param("followedId", idJperez.toString())
                        .cookie(cookieDeMlopez))
                .andReturn();

        // Verificación: responde 200 pero la relación de agomez no se toca
        assertEquals(200, estado(resultado), "Debe responder 200: el followerId de la URL se ignora");
        assertTrue(servicioTwitter.estaSiguiendo(idAgomez, idJperez),
                "La relación de otro usuario no debe eliminarse");
    }

    @Test
    @DisplayName("GET /follows/is-following devuelve true cuando existe la relación")
    void estaSiguiendo_devuelveTrueCuandoExisteLaRelacion() throws Exception {
        // Setup: agomez sigue a jperez en el escenario inicial
        // (escenario inicial)

        // Ejercitación: consultar la relación
        MvcResult resultado = consultarSiSigue(idAgomez, idJperez);

        // Verificación: responde 200 con true
        Boolean sigue = valorJson(resultado, "$");
        assertEquals(200, estado(resultado), "Debe responder 200");
        assertTrue(sigue, "agomez debe figurar siguiendo a jperez");
    }

    @Test
    @DisplayName("GET /follows/is-following devuelve false cuando no existe la relación")
    void estaSiguiendo_devuelveFalseCuandoNoExisteLaRelacion() throws Exception {
        // Setup: mlopez no sigue a agomez en el escenario inicial
        // (escenario inicial)

        // Ejercitación: consultar la relación
        MvcResult resultado = consultarSiSigue(idMlopez, idAgomez);

        // Verificación: responde 200 con false
        Boolean sigue = valorJson(resultado, "$");
        assertEquals(200, estado(resultado), "Debe responder 200");
        assertFalse(sigue, "mlopez no debe figurar siguiendo a agomez");
    }

    @Test
    @DisplayName("GET /follows/followers devuelve quiénes siguen al usuario")
    void seguidores_devuelveQuienesSiguenAlUsuario() throws Exception {
        // Setup: jperez tiene como seguidor a agomez en el escenario inicial
        // (escenario inicial)

        // Ejercitación: pedir los seguidores de jperez
        MvcResult resultado = mockMvc.perform(get("/follows/followers")
                        .param("userId", idJperez.toString()).cookie(cookie))
                .andReturn();

        // Verificación: responde 200 con agomez como único seguidor
        assertEquals(200, estado(resultado), "Debe responder 200");
        assertEquals(1, cantidadJson(resultado, "$"), "Debe tener un solo seguidor");
        assertEquals(AGOMEZ, valorJson(resultado, "$[0].username"), "El seguidor debe ser agomez");
    }

    @Test
    @DisplayName("GET /follows/following devuelve a quiénes sigue el usuario")
    void seguidos_devuelveAQuienesSigueElUsuario() throws Exception {
        // Setup: agomez sigue a jperez en el escenario inicial
        // (escenario inicial)

        // Ejercitación: pedir a quiénes sigue agomez
        MvcResult resultado = mockMvc.perform(get("/follows/following")
                        .param("userId", idAgomez.toString()).cookie(cookie))
                .andReturn();

        // Verificación: responde 200 con jperez como único seguido
        assertEquals(200, estado(resultado), "Debe responder 200");
        assertEquals(1, cantidadJson(resultado, "$"), "Debe seguir a una sola persona");
        assertEquals(JPEREZ, valorJson(resultado, "$[0].username"), "El seguido debe ser jperez");
    }

    @Test
    @DisplayName("GET /follows/following responde 401 cuando no se envía la cookie")
    void seguidos_responde401CuandoNoSeEnviaLaCookie() throws Exception {
        // Setup: un pedido sin cookie
        // (no se necesita setup adicional)

        // Ejercitación: pedir los seguidos sin sesión
        MvcResult resultado = mockMvc.perform(get("/follows/following").param("userId", idAgomez.toString()))
                .andReturn();

        // Verificación: 401 con el mensaje de no autenticado
        assertEquals(401, estado(resultado), "Sin cookie debe responder 401");
        assertEquals(GlobalExceptionHandler.ERROR_NO_AUTENTICADO, valorJson(resultado, "$.message"),
                "Debe informar que no está autenticado");
    }
}
