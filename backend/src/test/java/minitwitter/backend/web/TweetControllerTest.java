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

import static minitwitter.backend.service.TwitterService.ERROR_ELIMINAR_TWEET_AJENO;
import static minitwitter.backend.service.TwitterService.ERROR_PAGINA_NEGATIVA;
import static minitwitter.backend.service.TwitterService.ERROR_USUARIO_NO_ENCONTRADO;
import static minitwitter.backend.service.TwitterService.TAMANIO_PAGINA_FEED;
import static minitwitter.backend.util.DatosDePrueba.AGOMEZ;
import static minitwitter.backend.util.DatosDePrueba.CONTRASENIA_AGOMEZ;
import static minitwitter.backend.util.DatosDePrueba.ID_INEXISTENTE;
import static minitwitter.backend.util.DatosDePrueba.TWEET_DE_AGOMEZ;
import static minitwitter.backend.util.DatosDePrueba.TWEET_DE_JPEREZ;
import static minitwitter.backend.util.RespuestasHttp.cantidadJson;
import static minitwitter.backend.util.RespuestasHttp.estado;
import static minitwitter.backend.util.RespuestasHttp.valorJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(classes = Main.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TweetControllerTest {

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TwitterService servicioTwitter;

    private Integer idAgomez;
    private Integer idJperez;
    private Cookie cookie;

    @BeforeEach
    void antesDeCadaTest() {
        emf.getSchemaManager().truncate();
        DatosDePrueba.cargar(emf);
        idAgomez = DatosDePrueba.idUsuario(emf, AGOMEZ);
        idJperez = DatosDePrueba.idUsuario(emf, DatosDePrueba.JPEREZ);
        cookie = new Cookie(AuthController.NOMBRE_COOKIE, servicioTwitter.iniciarSesion(AGOMEZ, CONTRASENIA_AGOMEZ));
    }

    private MvcResult crearTweetPorHttp(String cuerpoJson, Cookie cookieDeSesion) throws Exception {
        var pedido = post("/tweets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoJson);
        if (cookieDeSesion != null) {
            pedido.cookie(cookieDeSesion);
        }
        return mockMvc.perform(pedido).andReturn();
    }

    // Cuerpo actual: solo el contenido (el autor sale de la cookie).
    private static String cuerpoDeTweet(String contenido) {
        return "{\"content\":\"" + contenido + "\"}";
    }

    // Cuerpo con el campo userId que todavía envía el frontend.
    private static String cuerpoDeTweetConUserId(Integer idUsuario, String contenido) {
        return "{\"userId\":" + idUsuario + ",\"content\":\"" + contenido + "\"}";
    }

    // ---------- POST /tweets ----------

    @Test
    @DisplayName("POST /tweets responde 200 cuando el tweet es válido")
    void crearTweet_responde200CuandoElTweetEsValido() throws Exception {
        // Setup: un usuario con sesión y un contenido válido
        // (la cookie se arma en el antesDeCadaTest)

        // Ejercitación: publicar el tweet por HTTP
        MvcResult resultado = crearTweetPorHttp(cuerpoDeTweet("Mi nuevo tweet"), cookie);

        // Verificación: responde 200 y el tweet queda entre los de la sesión
        assertEquals(200, estado(resultado), "Publicar un tweet válido debe responder 200");
        assertEquals(2, servicioTwitter.listarTweetsDelUsuario(idAgomez, 0).totalElements(),
                "El tweet nuevo debe sumarse al que ya tenía el usuario");
    }

    @Test
    @DisplayName("POST /tweets responde 401 cuando no se envía la cookie")
    void crearTweet_responde401CuandoNoSeEnviaLaCookie() throws Exception {
        // Setup: un pedido sin cookie
        // (no se necesita setup adicional)

        // Ejercitación: publicar el tweet sin sesión
        MvcResult resultado = crearTweetPorHttp(cuerpoDeTweet("Mi nuevo tweet"), null);

        // Verificación: responde 401
        assertEquals(401, estado(resultado), "Sin cookie debe responder 401");
    }

    @Test
    @DisplayName("POST /tweets publica a nombre de la sesión aunque el cuerpo traiga el userId de otro usuario")
    void crearTweet_publicaANombreDeLaSesionCuandoElCuerpoTraeOtroUserId() throws Exception {
        // Setup: sesión de agomez y un cuerpo que intenta publicar como jperez
        String cuerpo = cuerpoDeTweetConUserId(idJperez, "Intento de suplantación");

        // Ejercitación: publicar el tweet por HTTP
        MvcResult resultado = crearTweetPorHttp(cuerpo, cookie);

        // Verificación: el tweet queda de agomez y jperez no recibe ninguno
        assertEquals(200, estado(resultado), "Debe responder 200: el userId del cuerpo se ignora");
        assertEquals(2, servicioTwitter.listarTweetsDelUsuario(idAgomez, 0).totalElements(),
                "El tweet debe quedar a nombre del usuario de la sesión");
        assertEquals(1, servicioTwitter.listarTweetsDelUsuario(idJperez, 0).totalElements(),
                "El usuario del userId del cuerpo no debe recibir ningún tweet nuevo");
    }

    @Test
    @DisplayName("POST /tweets sigue aceptando el cuerpo con userId que envía el frontend")
    void crearTweet_aceptaElCuerpoConUserIdDelFrontend() throws Exception {
        // Setup: el cuerpo con el formato que envía hoy el frontend ({ userId, content })
        String cuerpo = cuerpoDeTweetConUserId(idAgomez, "Mi nuevo tweet");

        // Ejercitación: publicar el tweet por HTTP
        MvcResult resultado = crearTweetPorHttp(cuerpo, cookie);

        // Verificación: responde 200 y el tweet se publica
        assertEquals(200, estado(resultado), "El campo userId sobrante no debe hacer fallar el pedido");
        assertEquals(2, servicioTwitter.listarTweetsDelUsuario(idAgomez, 0).totalElements(),
                "El tweet debe publicarse");
    }

    @Test
    @DisplayName("POST /tweets responde 400 cuando el usuario de la sesión fue eliminado")
    void crearTweet_responde400CuandoElUsuarioDeLaSesionFueEliminado() throws Exception {
        // Setup: un usuario con sesión iniciada que después es eliminado
        servicioTwitter.eliminarUsuario(idAgomez);

        // Ejercitación: publicar el tweet con la cookie de ese usuario
        MvcResult resultado = crearTweetPorHttp(cuerpoDeTweet("Mi nuevo tweet"), cookie);

        // Verificación: 400 con el mensaje de usuario no encontrado
        assertEquals(400, estado(resultado), "Un usuario eliminado no puede publicar");
        assertEquals(ERROR_USUARIO_NO_ENCONTRADO + idAgomez, valorJson(resultado, "$.message"),
                "Debe informar que el usuario no fue encontrado");
    }

    // ---------- GET /tweets ----------

    @Test
    @DisplayName("GET /tweets devuelve los tweets originales del usuario")
    void listarTweets_devuelveLosTweetsOriginalesDelUsuario() throws Exception {
        // Setup: agomez tiene un tweet original y un retweet en el escenario inicial
        // (escenario inicial)

        // Ejercitación: pedir los tweets de agomez
        MvcResult resultado = mockMvc.perform(get("/tweets").param("userId", idAgomez.toString()).cookie(cookie))
                .andReturn();

        // Verificación: responde 200 con su único tweet original
        assertEquals(200, estado(resultado), "Debe responder 200");
        assertEquals(1, cantidadJson(resultado, "$.content"), "Solo debe devolver el tweet original");
        assertEquals(TWEET_DE_AGOMEZ, valorJson(resultado, "$.content[0].content"), "Debe ser el tweet de agomez");
    }

    @Test
    @DisplayName("GET /tweets responde 400 cuando falta el parámetro userId")
    void listarTweets_responde400CuandoFaltaElParametroUserId() throws Exception {
        // Setup: un pedido sin userId
        // (no se necesita setup adicional)

        // Ejercitación: pedir los tweets sin indicar de quién
        MvcResult resultado = mockMvc.perform(get("/tweets").cookie(cookie)).andReturn();

        // Verificación: 400 por parámetros inválidos
        assertEquals(400, estado(resultado), "Sin userId debe responder 400");
        assertEquals(GlobalExceptionHandler.ERROR_PARAMETROS_INVALIDOS, valorJson(resultado, "$.message"),
                "Debe informar que los parámetros son inválidos");
    }

    @Test
    @DisplayName("GET /tweets responde 400 cuando la página es negativa")
    void listarTweets_responde400CuandoLaPaginaEsNegativa() throws Exception {
        // Setup: una página negativa
        String paginaNegativa = "-1";

        // Ejercitación: pedir esa página
        MvcResult resultado = mockMvc.perform(get("/tweets")
                        .param("userId", idAgomez.toString()).param("page", paginaNegativa).cookie(cookie))
                .andReturn();

        // Verificación: 400 con el mensaje de página negativa
        assertEquals(400, estado(resultado), "Una página negativa debe responder 400");
        assertEquals(ERROR_PAGINA_NEGATIVA, valorJson(resultado, "$.message"),
                "Debe informar que la página no puede ser negativa");
    }

    @Test
    @DisplayName("GET /tweets responde 400 cuando la página no es un número")
    void listarTweets_responde400CuandoLaPaginaNoEsUnNumero() throws Exception {
        // Setup: una página que no es un número
        String paginaInvalida = "abc";

        // Ejercitación: pedir esa página
        MvcResult resultado = mockMvc.perform(get("/tweets")
                        .param("userId", idAgomez.toString()).param("page", paginaInvalida).cookie(cookie))
                .andReturn();

        // Verificación: 400 por parámetros inválidos
        assertEquals(400, estado(resultado), "Una página no numérica debe responder 400");
        assertEquals(GlobalExceptionHandler.ERROR_PARAMETROS_INVALIDOS, valorJson(resultado, "$.message"),
                "Debe informar que los parámetros son inválidos");
    }

    @Test
    @DisplayName("GET /tweets responde 401 cuando no se envía la cookie")
    void listarTweets_responde401CuandoNoSeEnviaLaCookie() throws Exception {
        // Setup: un pedido sin cookie
        // (no se necesita setup adicional)

        // Ejercitación: pedir los tweets sin sesión
        MvcResult resultado = mockMvc.perform(get("/tweets").param("userId", idAgomez.toString())).andReturn();

        // Verificación: 401 con el mensaje de no autenticado
        assertEquals(401, estado(resultado), "Sin cookie debe responder 401");
        assertEquals(GlobalExceptionHandler.ERROR_NO_AUTENTICADO, valorJson(resultado, "$.message"),
                "Debe informar que no está autenticado");
    }

    // ---------- GET /tweets/feed ----------

    @Test
    @DisplayName("GET /tweets/feed devuelve los tweets propios y los de los usuarios que sigue")
    void feed_devuelveLosTweetsPropiosYDeSeguidos() throws Exception {
        // Setup: agomez tiene su tweet y su retweet, y sigue a jperez (3 elementos en total)
        // (escenario inicial)

        // Ejercitación: pedir el feed
        MvcResult resultado = mockMvc.perform(get("/tweets/feed").cookie(cookie)).andReturn();

        // Verificación: responde 200 con los 3 elementos en una sola página
        Integer total = valorJson(resultado, "$.totalElements");
        Boolean hayMas = valorJson(resultado, "$.hasNext");
        assertEquals(200, estado(resultado), "Debe responder 200");
        assertEquals(3, total, "Debe contar los tweets propios, el retweet y el del seguido");
        assertFalse(hayMas, "Con 3 elementos no debe haber una página siguiente");
    }

    @Test
    @DisplayName("GET /tweets/feed devuelve la segunda página cuando se pide page=1")
    void feed_devuelveLaSegundaPaginaCuandoSePideElParametroPage() throws Exception {
        // Setup: agomez con 3 elementos iniciales más una página completa de tweets nuevos
        for (int i = 1; i <= TAMANIO_PAGINA_FEED; i++) {
            servicioTwitter.crearTweet(idAgomez, "Tweet número " + i);
        }

        // Ejercitación: pedir la página 1
        MvcResult resultado = mockMvc.perform(get("/tweets/feed").param("page", "1").cookie(cookie)).andReturn();

        // Verificación: trae los 3 elementos que sobran de la primera página
        Integer pagina = valorJson(resultado, "$.page");
        assertEquals(200, estado(resultado), "Debe responder 200");
        assertEquals(1, pagina, "Debe indicar que es la página 1");
        assertEquals(3, cantidadJson(resultado, "$.content"), "Debe traer solo el resto de elementos");
    }

    @Test
    @DisplayName("GET /tweets/feed responde 401 cuando no se envía la cookie")
    void feed_responde401CuandoNoSeEnviaLaCookie() throws Exception {
        // Setup: un pedido sin cookie
        // (no se necesita setup adicional)

        // Ejercitación: pedir el feed sin sesión
        MvcResult resultado = mockMvc.perform(get("/tweets/feed")).andReturn();

        // Verificación: 401 con el mensaje de no autenticado
        assertEquals(401, estado(resultado), "Sin cookie debe responder 401");
        assertEquals(GlobalExceptionHandler.ERROR_NO_AUTENTICADO, valorJson(resultado, "$.message"),
                "Debe informar que no está autenticado");
    }

    @Test
    @DisplayName("GET /tweets/feed mantiene el retweet cuando el tweet original fue borrado")
    void feed_mantieneElRetweetCuandoElTweetOriginalFueBorrado() throws Exception {
        // Setup: jperez borra el tweet que agomez había retuiteado
        servicioTwitter.eliminarTweet(idJperez, DatosDePrueba.idTweet(emf, TWEET_DE_JPEREZ));

        // Ejercitación: pedir el feed de agomez
        MvcResult resultado = mockMvc.perform(get("/tweets/feed").cookie(cookie)).andReturn();

        // Verificación: el retweet sigue en el feed, marcado con el original borrado
        assertEquals(200, estado(resultado), "Debe responder 200");
        assertEquals(1, cantidadJson(resultado, "$.content[?(@.originalDeleted == true)]"),
                "El retweet debe seguir apareciendo con el original no disponible");
    }

    // ---------- DELETE /tweets/{id} ----------

    @Test
    @DisplayName("DELETE /tweets/{id} responde 200 cuando el autor elimina su tweet")
    void eliminarTweet_responde200CuandoElAutorLoElimina() throws Exception {
        // Setup: el tweet de agomez
        Integer idTweet = DatosDePrueba.idTweet(emf, TWEET_DE_AGOMEZ);

        // Ejercitación: el autor lo elimina por HTTP
        MvcResult resultado = mockMvc.perform(delete("/tweets/" + idTweet).cookie(cookie)).andReturn();

        // Verificación: responde 200 y el tweet deja de listarse
        assertEquals(200, estado(resultado), "Eliminar el propio tweet debe responder 200");
        assertEquals(0, servicioTwitter.listarTweetsDelUsuario(idAgomez, 0).totalElements(),
                "El tweet eliminado no debe listarse");
    }

    @Test
    @DisplayName("DELETE /tweets/{id} responde 403 cuando el tweet es de otro usuario")
    void eliminarTweet_responde403CuandoNoEsElAutor() throws Exception {
        // Setup: el tweet de jperez, que intenta borrar agomez
        Integer idTweet = DatosDePrueba.idTweet(emf, TWEET_DE_JPEREZ);

        // Ejercitación: intentar eliminarlo por HTTP
        MvcResult resultado = mockMvc.perform(delete("/tweets/" + idTweet).cookie(cookie)).andReturn();

        // Verificación: 403 con el mensaje de tweet ajeno
        assertEquals(403, estado(resultado), "Eliminar un tweet ajeno debe responder 403");
        assertEquals(ERROR_ELIMINAR_TWEET_AJENO, valorJson(resultado, "$.message"),
                "Debe informar que no se puede eliminar un tweet de otra persona");
    }

    @Test
    @DisplayName("DELETE /tweets/{id} responde 200 cuando el tweet no existe")
    void eliminarTweet_responde200CuandoElTweetNoExiste() throws Exception {
        // Setup: un id que no corresponde a ningún tweet
        // (ID_INEXISTENTE)

        // Ejercitación: eliminarlo por HTTP
        MvcResult resultado = mockMvc.perform(delete("/tweets/" + ID_INEXISTENTE).cookie(cookie)).andReturn();

        // Verificación: responde 200 sin hacer nada
        assertEquals(200, estado(resultado), "Eliminar un tweet inexistente no debe fallar");
    }

    @Test
    @DisplayName("DELETE /tweets/{id} responde 401 cuando no se envía la cookie")
    void eliminarTweet_responde401CuandoNoSeEnviaLaCookie() throws Exception {
        // Setup: un pedido sin cookie
        Integer idTweet = DatosDePrueba.idTweet(emf, TWEET_DE_AGOMEZ);

        // Ejercitación: eliminar sin sesión
        MvcResult resultado = mockMvc.perform(delete("/tweets/" + idTweet)).andReturn();

        // Verificación: responde 401
        assertEquals(401, estado(resultado), "Sin cookie debe responder 401");
    }
}
