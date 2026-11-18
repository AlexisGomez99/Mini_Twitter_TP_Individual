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

import static minitwitter.backend.service.TwitterService.ERROR_ELIMINAR_RETWEET_AJENO;
import static minitwitter.backend.service.TwitterService.ERROR_TWEET_NO_ENCONTRADO;
import static minitwitter.backend.util.DatosDePrueba.AGOMEZ;
import static minitwitter.backend.util.DatosDePrueba.CONTRASENIA_AGOMEZ;
import static minitwitter.backend.util.DatosDePrueba.CONTRASENIA_MLOPEZ;
import static minitwitter.backend.util.DatosDePrueba.ID_INEXISTENTE;
import static minitwitter.backend.util.DatosDePrueba.MLOPEZ;
import static minitwitter.backend.util.DatosDePrueba.TWEET_DE_JPEREZ;
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
class RetweetControllerTest {

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TwitterService servicioTwitter;

    private Integer idAgomez;
    private Integer idMlopez;
    private Integer idTweetDeJperez;
    private Cookie cookie;
    private Cookie cookieDeMlopez;

    @BeforeEach
    void antesDeCadaTest() {
        emf.getSchemaManager().truncate();
        DatosDePrueba.cargar(emf);
        idAgomez = DatosDePrueba.idUsuario(emf, AGOMEZ);
        idMlopez = DatosDePrueba.idUsuario(emf, MLOPEZ);
        idTweetDeJperez = DatosDePrueba.idTweet(emf, TWEET_DE_JPEREZ);
        cookie = new Cookie(AuthController.NOMBRE_COOKIE, servicioTwitter.iniciarSesion(AGOMEZ, CONTRASENIA_AGOMEZ));
        cookieDeMlopez = new Cookie(AuthController.NOMBRE_COOKIE, servicioTwitter.iniciarSesion(MLOPEZ, CONTRASENIA_MLOPEZ));
    }

    private MvcResult crearRetweetPorHttp(String cuerpoJson, Cookie cookieDeSesion) throws Exception {
        var pedido = post("/retweets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoJson);
        if (cookieDeSesion != null) {
            pedido.cookie(cookieDeSesion);
        }
        return mockMvc.perform(pedido).andReturn();
    }

    // Cuerpo actual: solo el tweet de origen (quien retuitea sale de la cookie).
    private static String cuerpoDeRetweet(Integer idTweetOrigen) {
        return "{\"originTweetId\":" + idTweetOrigen + "}";
    }

    @Test
    @DisplayName("POST /retweets responde 200 cuando el retweet es válido")
    void crearRetweet_responde200CuandoElRetweetEsValido() throws Exception {
        // Setup: mlopez, con sesión iniciada, retuitea el tweet de jperez
        // (los ids y la cookie se obtienen en el antesDeCadaTest)

        // Ejercitación: crear el retweet por HTTP
        MvcResult resultado = crearRetweetPorHttp(cuerpoDeRetweet(idTweetDeJperez), cookieDeMlopez);

        // Verificación: responde 200 y el retweet queda registrado
        assertEquals(200, estado(resultado), "Crear un retweet válido debe responder 200");
        assertEquals(1, servicioTwitter.listarRetweetsDelUsuario(idMlopez).size(),
                "El retweet debe quedar registrado");
    }

    @Test
    @DisplayName("POST /retweets crea el retweet a nombre de la sesión aunque el cuerpo traiga el userId de otro usuario")
    void crearRetweet_creaElRetweetANombreDeLaSesionCuandoElCuerpoTraeOtroUserId() throws Exception {
        // Setup: sesión de mlopez y un cuerpo que intenta retuitear como agomez
        String cuerpo = "{\"userId\":" + idAgomez + ",\"originTweetId\":" + idTweetDeJperez + "}";

        // Ejercitación: crear el retweet por HTTP
        MvcResult resultado = crearRetweetPorHttp(cuerpo, cookieDeMlopez);

        // Verificación: el retweet queda de mlopez y agomez conserva solo el suyo inicial
        assertEquals(200, estado(resultado), "Debe responder 200: el userId del cuerpo se ignora");
        assertEquals(1, servicioTwitter.listarRetweetsDelUsuario(idMlopez).size(),
                "El retweet debe quedar a nombre del usuario de la sesión");
        assertEquals(1, servicioTwitter.listarRetweetsDelUsuario(idAgomez).size(),
                "El usuario del userId del cuerpo no debe recibir ningún retweet nuevo");
    }

    @Test
    @DisplayName("POST /retweets responde 400 cuando el tweet de origen no existe")
    void crearRetweet_responde400CuandoElTweetNoExiste() throws Exception {
        // Setup: un id de tweet inexistente
        // (ID_INEXISTENTE)

        // Ejercitación: crear el retweet por HTTP
        MvcResult resultado = crearRetweetPorHttp(cuerpoDeRetweet(ID_INEXISTENTE), cookieDeMlopez);

        // Verificación: 400 con el mensaje de tweet no encontrado
        assertEquals(400, estado(resultado), "Un tweet inexistente debe responder 400");
        assertEquals(ERROR_TWEET_NO_ENCONTRADO + ID_INEXISTENTE, valorJson(resultado, "$.message"),
                "Debe informar que el tweet no fue encontrado");
    }

    @Test
    @DisplayName("POST /retweets responde 401 cuando no se envía la cookie")
    void crearRetweet_responde401CuandoNoSeEnviaLaCookie() throws Exception {
        // Setup: un pedido sin cookie
        // (no se necesita setup adicional)

        // Ejercitación: crear el retweet sin sesión
        MvcResult resultado = crearRetweetPorHttp(cuerpoDeRetweet(idTweetDeJperez), null);

        // Verificación: responde 401
        assertEquals(401, estado(resultado), "Sin cookie debe responder 401");
    }

    @Test
    @DisplayName("GET /retweets devuelve los retweets del usuario")
    void listarRetweets_devuelveLosRetweetsDelUsuario() throws Exception {
        // Setup: agomez tiene un retweet del tweet de jperez en el escenario inicial
        // (escenario inicial)

        // Ejercitación: pedir los retweets de agomez
        MvcResult resultado = mockMvc.perform(get("/retweets").param("userId", idAgomez.toString()).cookie(cookie))
                .andReturn();

        // Verificación: responde 200 con su retweet
        Integer idOrigen = valorJson(resultado, "$[0].originalTweetId");
        assertEquals(200, estado(resultado), "Debe responder 200");
        assertEquals(1, cantidadJson(resultado, "$"), "Debe devolver un solo retweet");
        assertEquals(idTweetDeJperez, idOrigen, "El retweet debe apuntar al tweet de jperez");
    }

    @Test
    @DisplayName("GET /retweets responde 401 cuando no se envía la cookie")
    void listarRetweets_responde401CuandoNoSeEnviaLaCookie() throws Exception {
        // Setup: un pedido sin cookie
        // (no se necesita setup adicional)

        // Ejercitación: pedir los retweets sin sesión
        MvcResult resultado = mockMvc.perform(get("/retweets").param("userId", idAgomez.toString())).andReturn();

        // Verificación: responde 401
        assertEquals(401, estado(resultado), "Sin cookie debe responder 401");
    }

    @Test
    @DisplayName("DELETE /retweets/{id} responde 200 y saca el retweet de la lista")
    void eliminarRetweet_responde200YSacaElRetweetDeLaLista() throws Exception {
        // Setup: el retweet de agomez
        Integer idRetweet = DatosDePrueba.idRetweetDe(emf, AGOMEZ);

        // Ejercitación: eliminarlo por HTTP
        MvcResult resultado = mockMvc.perform(delete("/retweets/" + idRetweet).cookie(cookie)).andReturn();

        // Verificación: responde 200 y agomez ya no tiene retweets
        assertEquals(200, estado(resultado), "Eliminar un retweet debe responder 200");
        assertTrue(servicioTwitter.listarRetweetsDelUsuario(idAgomez).isEmpty(),
                "El retweet eliminado no debe listarse");
    }

    @Test
    @DisplayName("DELETE /retweets/{id} responde 403 cuando el retweet es de otro usuario")
    void eliminarRetweet_responde403CuandoNoEsElAutor() throws Exception {
        // Setup: el retweet de agomez, que intenta borrar mlopez
        Integer idRetweet = DatosDePrueba.idRetweetDe(emf, AGOMEZ);

        // Ejercitación: intentar eliminarlo por HTTP
        MvcResult resultado = mockMvc.perform(delete("/retweets/" + idRetweet).cookie(cookieDeMlopez)).andReturn();

        // Verificación: 403 con el mensaje de retweet ajeno y el retweet sigue existiendo
        assertEquals(403, estado(resultado), "Eliminar un retweet ajeno debe responder 403");
        assertEquals(ERROR_ELIMINAR_RETWEET_AJENO, valorJson(resultado, "$.message"),
                "Debe informar que no se puede eliminar un retweet de otra persona");
        assertEquals(1, servicioTwitter.listarRetweetsDelUsuario(idAgomez).size(),
                "El retweet ajeno no debe borrarse");
    }
}
