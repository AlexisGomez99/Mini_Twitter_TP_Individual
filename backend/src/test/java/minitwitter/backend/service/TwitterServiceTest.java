package minitwitter.backend.service;

import jakarta.persistence.EntityManagerFactory;
import minitwitter.backend.dto.OriginalTweetInfo;
import minitwitter.backend.dto.PageResponse;
import minitwitter.backend.dto.TweetInfo;
import minitwitter.backend.model.DomainException;
import minitwitter.backend.util.DatosDePrueba;
import minitwitter.backend.util.EmfBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static minitwitter.backend.service.TwitterService.ERROR_CREDENCIALES_INCORRECTAS;
import static minitwitter.backend.service.TwitterService.ERROR_ELIMINAR_RETWEET_AJENO;
import static minitwitter.backend.service.TwitterService.ERROR_ELIMINAR_TWEET_AJENO;
import static minitwitter.backend.service.TwitterService.ERROR_PAGINA_NEGATIVA;
import static minitwitter.backend.service.TwitterService.ERROR_TOKEN_INVALIDO;
import static minitwitter.backend.service.TwitterService.ERROR_TWEET_NO_ENCONTRADO;
import static minitwitter.backend.service.TwitterService.ERROR_USUARIO_NO_ENCONTRADO;
import static minitwitter.backend.service.TwitterService.ERROR_USUARIO_YA_EXISTE;
import static minitwitter.backend.service.TwitterService.TAMANIO_PAGINA_FEED;
import static minitwitter.backend.service.TwitterService.TAMANIO_PAGINA_TWEETS;
import static minitwitter.backend.util.DatosDePrueba.AGOMEZ;
import static minitwitter.backend.util.DatosDePrueba.CONTRASENIA_AGOMEZ;
import static minitwitter.backend.util.DatosDePrueba.ID_INEXISTENTE;
import static minitwitter.backend.util.DatosDePrueba.MLOPEZ;
import static minitwitter.backend.util.DatosDePrueba.TWEET_DE_AGOMEZ;
import static minitwitter.backend.util.DatosDePrueba.TWEET_DE_JPEREZ;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TwitterServiceTest {

    private static EntityManagerFactory emf;

    private TwitterService servicio;
    private Integer idAgomez;
    private Integer idJperez;
    private Integer idMlopez;

    @BeforeAll
    static void antesDeTodo() {
        emf = new EmfBuilder().h2EnMemoria().conRecreacionDeEsquema().construir();
    }

    @AfterAll
    static void despuesDeTodo() {
        emf.close();
    }

    @BeforeEach
    void antesDeCadaTest() {
        emf.getSchemaManager().truncate();
        DatosDePrueba.cargar(emf);
        servicio = new TwitterService(emf);
        idAgomez = DatosDePrueba.idUsuario(emf, AGOMEZ);
        idJperez = DatosDePrueba.idUsuario(emf, DatosDePrueba.JPEREZ);
        idMlopez = DatosDePrueba.idUsuario(emf, MLOPEZ);
    }

    private void crearTweets(Integer idUsuario, int cantidad) {
        for (int i = 1; i <= cantidad; i++) {
            servicio.crearTweet(idUsuario, "Tweet número " + i);
        }
    }

    // ---------- Autenticación ----------

    @Test
    @DisplayName("Iniciar sesión con credenciales correctas devuelve un token del usuario")
    void iniciarSesion_devuelveUnTokenDelUsuarioCuandoLasCredencialesSonCorrectas() {
        // Setup: credenciales de un usuario existente
        // (AGOMEZ y su contraseña)

        // Ejercitación: iniciar sesión
        String token = servicio.iniciarSesion(AGOMEZ, CONTRASENIA_AGOMEZ);

        // Verificación: el token identifica al usuario
        assertNotNull(token, "Debe devolver un token");
        assertEquals(idAgomez, servicio.verificarTokenYObtenerIdUsuario(token),
                "El token debe contener el id del usuario que inició sesión");
    }

    @Test
    @DisplayName("Iniciar sesión lanza excepción 401 cuando la contraseña es incorrecta")
    void iniciarSesion_lanzaExcepcionCuandoLaContraseniaEsIncorrecta() {
        // Setup: contraseña equivocada para un usuario existente
        String contraseniaIncorrecta = "otra_clave";

        // Ejercitación y Verificación
        var excepcion = assertThrows(ResponseStatusException.class,
                () -> servicio.iniciarSesion(AGOMEZ, contraseniaIncorrecta));
        assertEquals(HttpStatus.UNAUTHORIZED, excepcion.getStatusCode(), "Debe responder 401");
        assertEquals(ERROR_CREDENCIALES_INCORRECTAS, excepcion.getReason(),
                "Debe informar que las credenciales son incorrectas");
    }

    @Test
    @DisplayName("Iniciar sesión lanza excepción 401 cuando el usuario no existe")
    void iniciarSesion_lanzaExcepcionCuandoElUsuarioNoExiste() {
        // Setup: un nombre que no está registrado
        String nombreInexistente = "no_existe";

        // Ejercitación y Verificación
        var excepcion = assertThrows(ResponseStatusException.class,
                () -> servicio.iniciarSesion(nombreInexistente, CONTRASENIA_AGOMEZ));
        assertEquals(HttpStatus.UNAUTHORIZED, excepcion.getStatusCode(), "Debe responder 401");
        assertEquals(ERROR_CREDENCIALES_INCORRECTAS, excepcion.getReason(),
                "Debe informar que las credenciales son incorrectas");
    }

    @Test
    @DisplayName("Iniciar sesión lanza excepción 401 cuando el nombre es null")
    void iniciarSesion_lanzaExcepcionCuandoElNombreEsNull() {
        // Setup: nombre nulo
        // (no se necesita setup adicional)

        // Ejercitación y Verificación
        var excepcion = assertThrows(ResponseStatusException.class,
                () -> servicio.iniciarSesion(null, CONTRASENIA_AGOMEZ));
        assertEquals(HttpStatus.UNAUTHORIZED, excepcion.getStatusCode(), "Debe responder 401");
    }

    @Test
    @DisplayName("Verificar un token inválido lanza excepción 401")
    void verificarTokenYObtenerIdUsuario_lanzaExcepcionCuandoElTokenEsInvalido() {
        // Setup: un texto que no es un token
        String tokenInvalido = "esto-no-es-un-token";

        // Ejercitación y Verificación
        var excepcion = assertThrows(ResponseStatusException.class,
                () -> servicio.verificarTokenYObtenerIdUsuario(tokenInvalido));
        assertEquals(HttpStatus.UNAUTHORIZED, excepcion.getStatusCode(), "Debe responder 401");
        assertEquals(ERROR_TOKEN_INVALIDO, excepcion.getReason(), "Debe informar que el token es inválido");
    }

    @Test
    @DisplayName("Verificar un token vacío lanza excepción 401")
    void verificarTokenYObtenerIdUsuario_lanzaExcepcionCuandoElTokenEstaVacio() {
        // Setup: token vacío
        String tokenVacio = "";

        // Ejercitación y Verificación
        var excepcion = assertThrows(ResponseStatusException.class,
                () -> servicio.verificarTokenYObtenerIdUsuario(tokenVacio));
        assertEquals(ERROR_TOKEN_INVALIDO, excepcion.getReason(), "Debe informar que el token es inválido");
    }

    @Test
    @DisplayName("Registrar un usuario nuevo lo crea y devuelve su id")
    void registrarUsuario_creaElUsuarioYDevuelveSuId() {
        // Setup: un nombre que todavía no existe
        String nombreNuevo = "lgomez";

        // Ejercitación: registrar el usuario
        Integer id = servicio.registrarUsuario(nombreNuevo, "clave123");

        // Verificación: el usuario existe con ese id
        var usuario = servicio.obtenerUsuarioPorId(id);
        assertTrue(usuario.isPresent(), "El usuario registrado debe poder consultarse");
        assertEquals(nombreNuevo, usuario.get().username(), "Debe tener el nombre con el que se registró");
    }

    @Test
    @DisplayName("Registrar un usuario lanza excepción cuando el nombre ya existe")
    void registrarUsuario_lanzaExcepcionCuandoElNombreYaExiste() {
        // Setup: un nombre que ya pertenece a un usuario activo
        // (AGOMEZ)

        // Ejercitación y Verificación
        var excepcion = assertThrows(RuntimeException.class,
                () -> servicio.registrarUsuario(AGOMEZ, "otra_clave"));
        assertEquals(ERROR_USUARIO_YA_EXISTE, excepcion.getMessage(), "Debe informar que el nombre ya existe");
    }

    @Test
    @DisplayName("Registrar un usuario lanza excepción cuando el nombre pertenece a una cuenta borrada")
    void registrarUsuario_lanzaExcepcionCuandoElNombreEsDeUnaCuentaBorrada() {
        // Setup: mlopez fue borrado, pero su nombre sigue ocupado en la base
        servicio.eliminarUsuario(idMlopez);

        // Ejercitación y Verificación
        var excepcion = assertThrows(RuntimeException.class,
                () -> servicio.registrarUsuario(MLOPEZ, "clave123"));
        assertEquals(ERROR_USUARIO_YA_EXISTE, excepcion.getMessage(), "El nombre de una cuenta borrada no debe reutilizarse");
    }

    // ---------- Usuarios ----------

    @Test
    @DisplayName("Obtener un usuario por id devuelve vacío cuando no existe")
    void obtenerUsuarioPorId_devuelveVacioCuandoElUsuarioNoExiste() {
        // Setup: un id que no corresponde a ningún usuario
        // (ID_INEXISTENTE)

        // Ejercitación: buscarlo
        var usuario = servicio.obtenerUsuarioPorId(ID_INEXISTENTE);

        // Verificación: no se encuentra
        assertTrue(usuario.isEmpty(), "No debe encontrar un usuario inexistente");
    }

    @Test
    @DisplayName("Obtener un usuario por id devuelve vacío cuando fue eliminado")
    void obtenerUsuarioPorId_devuelveVacioCuandoElUsuarioFueEliminado() {
        // Setup: mlopez fue eliminado
        servicio.eliminarUsuario(idMlopez);

        // Ejercitación: buscarlo por id
        var usuario = servicio.obtenerUsuarioPorId(idMlopez);

        // Verificación: un usuario eliminado no se devuelve
        assertTrue(usuario.isEmpty(), "No debe devolver a un usuario eliminado");
    }

    @Test
    @DisplayName("Obtener un usuario por nombre devuelve vacío cuando fue eliminado")
    void obtenerUsuarioPorNombreUsuario_devuelveVacioCuandoElUsuarioFueEliminado() {
        // Setup: mlopez fue eliminado
        servicio.eliminarUsuario(idMlopez);

        // Ejercitación: buscarlo por nombre
        var usuario = servicio.obtenerUsuarioPorNombreUsuario(MLOPEZ);

        // Verificación: un usuario eliminado no se devuelve
        assertTrue(usuario.isEmpty(), "No debe devolver a un usuario eliminado");
    }

    @Test
    @DisplayName("Eliminar un usuario inexistente no falla")
    void eliminarUsuario_noFallaCuandoElUsuarioNoExiste() {
        // Setup: un id que no corresponde a ningún usuario
        // (ID_INEXISTENTE)

        // Ejercitación y Verificación
        assertDoesNotThrow(() -> servicio.eliminarUsuario(ID_INEXISTENTE),
                "Eliminar un usuario que no existe no debe lanzar excepción");
    }

    // ---------- Tweets ----------

    @Test
    @DisplayName("Crear un tweet para un usuario existente lo deja listado entre sus tweets")
    void crearTweet_dejaElTweetListadoParaUnUsuarioExistente() {
        // Setup: un usuario existente y un contenido
        String contenido = "Mi nuevo tweet";

        // Ejercitación: crear el tweet
        servicio.crearTweet(idMlopez, contenido);

        // Verificación: aparece entre los tweets del usuario
        PageResponse<OriginalTweetInfo> tweets = servicio.listarTweetsDelUsuario(idMlopez, 0);
        assertTrue(tweets.content().stream().anyMatch(tweet -> tweet.content().equals(contenido)),
                "El tweet creado debe listarse entre los del usuario");
    }

    @Test
    @DisplayName("Crear un tweet lanza excepción cuando el usuario no existe")
    void crearTweet_lanzaExcepcionCuandoElUsuarioNoExiste() {
        // Setup: un id que no corresponde a ningún usuario
        // (ID_INEXISTENTE)

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> servicio.crearTweet(ID_INEXISTENTE, "contenido"));
        assertEquals(ERROR_USUARIO_NO_ENCONTRADO + ID_INEXISTENTE, excepcion.getMessage(),
                "Debe informar que el usuario no fue encontrado");
    }

    @Test
    @DisplayName("Crear un tweet lanza excepción cuando el usuario está borrado")
    void crearTweet_lanzaExcepcionCuandoElUsuarioEstaBorrado() {
        // Setup: mlopez fue eliminado
        servicio.eliminarUsuario(idMlopez);

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> servicio.crearTweet(idMlopez, "contenido"));
        assertEquals(ERROR_USUARIO_NO_ENCONTRADO + idMlopez, excepcion.getMessage(),
                "Un usuario borrado no debe poder publicar");
    }

    @Test
    @DisplayName("Eliminar un tweet lo saca de la lista cuando quien lo pide es el autor")
    void eliminarTweet_loSacaDeLaListaCuandoQuienPideEsElAutor() {
        // Setup: el tweet de agomez
        Integer idTweet = DatosDePrueba.idTweet(emf, TWEET_DE_AGOMEZ);

        // Ejercitación: el autor lo elimina
        servicio.eliminarTweet(idAgomez, idTweet);

        // Verificación: ya no aparece entre sus tweets
        assertEquals(0, servicio.listarTweetsDelUsuario(idAgomez, 0).totalElements(),
                "El tweet eliminado no debe listarse");
    }

    @Test
    @DisplayName("Eliminar un tweet lanza excepción 403 cuando quien lo pide no es el autor")
    void eliminarTweet_lanzaExcepcionCuandoQuienPideNoEsElAutor() {
        // Setup: el tweet de jperez, que intenta borrar agomez
        Integer idTweet = DatosDePrueba.idTweet(emf, TWEET_DE_JPEREZ);

        // Ejercitación y Verificación
        var excepcion = assertThrows(ResponseStatusException.class,
                () -> servicio.eliminarTweet(idAgomez, idTweet));
        assertEquals(HttpStatus.FORBIDDEN, excepcion.getStatusCode(), "Debe responder 403");
        assertEquals(ERROR_ELIMINAR_TWEET_AJENO, excepcion.getReason(),
                "Debe informar que no se puede eliminar un tweet ajeno");
    }

    @Test
    @DisplayName("Eliminar el tweet de otra persona no lo borra")
    void eliminarTweet_noBorraElTweetCuandoQuienPideNoEsElAutor() {
        // Setup: el tweet de jperez
        Integer idTweet = DatosDePrueba.idTweet(emf, TWEET_DE_JPEREZ);

        // Ejercitación: agomez intenta borrarlo (el pedido es rechazado)
        assertThrows(ResponseStatusException.class, () -> servicio.eliminarTweet(idAgomez, idTweet));

        // Verificación: el tweet sigue listado
        assertEquals(1, servicio.listarTweetsDelUsuario(idJperez, 0).totalElements(),
                "El tweet de jperez debe seguir existiendo");
    }

    @Test
    @DisplayName("Eliminar un tweet inexistente no falla")
    void eliminarTweet_noFallaCuandoElTweetNoExiste() {
        // Setup: un id que no corresponde a ningún tweet
        // (ID_INEXISTENTE)

        // Ejercitación y Verificación
        assertDoesNotThrow(() -> servicio.eliminarTweet(idAgomez, ID_INEXISTENTE),
                "Eliminar un tweet que no existe no debe lanzar excepción");
    }

    @Test
    @DisplayName("Eliminar un tweet ignora el pedido cuando el id corresponde a un retweet")
    void eliminarTweet_ignoraElPedidoCuandoElIdEsDeUnRetweet() {
        // Setup: el id del retweet de agomez
        Integer idRetweet = DatosDePrueba.idRetweetDe(emf, AGOMEZ);

        // Ejercitación: pedir eliminarlo como si fuera un tweet
        servicio.eliminarTweet(idAgomez, idRetweet);

        // Verificación: el retweet sigue existiendo
        assertEquals(1, servicio.listarRetweetsDelUsuario(idAgomez).size(),
                "Un retweet no se elimina con eliminarTweet");
    }

    @Test
    @DisplayName("Listar tweets devuelve la primera página con el tamaño configurado")
    void listarTweetsDelUsuario_devuelveLaPrimeraPaginaConElTamanioConfigurado() {
        // Setup: mlopez con un tweet más que el tamaño de página
        crearTweets(idMlopez, TAMANIO_PAGINA_TWEETS);

        // Ejercitación: pedir la página 0
        var pagina = servicio.listarTweetsDelUsuario(idMlopez, 0);

        // Verificación: la página viene completa y hay otra después
        assertEquals(TAMANIO_PAGINA_TWEETS, pagina.content().size(), "La página debe traer el tamaño configurado");
        assertTrue(pagina.hasNext(), "Debe haber una página siguiente");
    }

    @Test
    @DisplayName("Listar tweets devuelve el resto en la segunda página")
    void listarTweetsDelUsuario_devuelveElRestoEnLaSegundaPagina() {
        // Setup: mlopez con un tweet más que el tamaño de página
        crearTweets(idMlopez, TAMANIO_PAGINA_TWEETS);

        // Ejercitación: pedir la página 1
        var pagina = servicio.listarTweetsDelUsuario(idMlopez, 1);

        // Verificación: trae el tweet que sobra y no hay más páginas
        assertEquals(1, pagina.content().size(), "La segunda página debe traer solo el resto");
        assertFalse(pagina.hasNext(), "No debe haber una página siguiente");
    }

    @Test
    @DisplayName("Listar tweets devuelve una página vacía cuando el usuario no tiene tweets")
    void listarTweetsDelUsuario_devuelvePaginaVaciaCuandoElUsuarioNoTieneTweets() {
        // Setup: un usuario recién registrado
        Integer idNuevo = servicio.registrarUsuario("novato01", "clave123");

        // Ejercitación: listar sus tweets
        var pagina = servicio.listarTweetsDelUsuario(idNuevo, 0);

        // Verificación: la página está vacía
        assertTrue(pagina.content().isEmpty(), "No debe haber tweets para listar");
    }

    @Test
    @DisplayName("Listar tweets lanza excepción cuando la página es negativa")
    void listarTweetsDelUsuario_lanzaExcepcionCuandoLaPaginaEsNegativa() {
        // Setup: una página negativa
        int paginaNegativa = -1;

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> servicio.listarTweetsDelUsuario(idAgomez, paginaNegativa));
        assertEquals(ERROR_PAGINA_NEGATIVA, excepcion.getMessage(), "Debe informar que la página no puede ser negativa");
    }

    @Test
    @DisplayName("Obtener el feed devuelve la primera página con el tamaño configurado")
    void obtenerFeedDelUsuario_devuelveLaPrimeraPaginaConElTamanioConfigurado() {
        // Setup: agomez con más elementos en el feed que el tamaño de página (3 iniciales + 20 nuevos)
        crearTweets(idAgomez, TAMANIO_PAGINA_FEED);

        // Ejercitación: pedir la página 0
        PageResponse<TweetInfo> pagina = servicio.obtenerFeedDelUsuario(idAgomez, 0);

        // Verificación: la página viene completa y hay otra después
        assertEquals(TAMANIO_PAGINA_FEED, pagina.content().size(), "La página debe traer el tamaño configurado");
        assertTrue(pagina.hasNext(), "Debe haber una página siguiente");
    }

    @Test
    @DisplayName("Obtener el feed devuelve el resto en la segunda página")
    void obtenerFeedDelUsuario_devuelveElRestoEnLaSegundaPagina() {
        // Setup: agomez con 3 elementos iniciales más 20 nuevos
        crearTweets(idAgomez, TAMANIO_PAGINA_FEED);

        // Ejercitación: pedir la página 1
        PageResponse<TweetInfo> pagina = servicio.obtenerFeedDelUsuario(idAgomez, 1);

        // Verificación: trae los 3 elementos que sobran y no hay más páginas
        assertEquals(3, pagina.content().size(), "La segunda página debe traer solo el resto");
        assertFalse(pagina.hasNext(), "No debe haber una página siguiente");
    }

    @Test
    @DisplayName("Obtener el feed devuelve una página vacía cuando no hay tweets propios ni seguidos")
    void obtenerFeedDelUsuario_devuelvePaginaVaciaCuandoNoHayTweetsNiSeguidos() {
        // Setup: un usuario recién registrado
        Integer idNuevo = servicio.registrarUsuario("novato01", "clave123");

        // Ejercitación: pedir su feed
        var pagina = servicio.obtenerFeedDelUsuario(idNuevo, 0);

        // Verificación: el feed está vacío
        assertTrue(pagina.content().isEmpty(), "El feed no debe tener elementos");
    }

    @Test
    @DisplayName("Obtener el feed lanza excepción cuando la página es negativa")
    void obtenerFeedDelUsuario_lanzaExcepcionCuandoLaPaginaEsNegativa() {
        // Setup: una página negativa
        int paginaNegativa = -1;

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> servicio.obtenerFeedDelUsuario(idAgomez, paginaNegativa));
        assertEquals(ERROR_PAGINA_NEGATIVA, excepcion.getMessage(), "Debe informar que la página no puede ser negativa");
    }

    // ---------- Retweets ----------

    @Test
    @DisplayName("Crear un retweet de un tweet existente lo deja listado entre los retweets del usuario")
    void crearRetweet_dejaElRetweetListadoParaUnUsuarioYUnTweetExistentes() {
        // Setup: mlopez y el tweet de jperez
        Integer idTweet = DatosDePrueba.idTweet(emf, TWEET_DE_JPEREZ);

        // Ejercitación: mlopez retuitea el tweet
        servicio.crearRetweet(idMlopez, idTweet);

        // Verificación: aparece entre los retweets de mlopez
        assertEquals(1, servicio.listarRetweetsDelUsuario(idMlopez).size(),
                "mlopez debe tener el retweet recién creado");
    }

    @Test
    @DisplayName("Crear un retweet lanza excepción cuando el usuario no existe")
    void crearRetweet_lanzaExcepcionCuandoElUsuarioNoExiste() {
        // Setup: un id de usuario inexistente y un tweet real
        Integer idTweet = DatosDePrueba.idTweet(emf, TWEET_DE_JPEREZ);

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> servicio.crearRetweet(ID_INEXISTENTE, idTweet));
        assertEquals(ERROR_USUARIO_NO_ENCONTRADO + ID_INEXISTENTE, excepcion.getMessage(),
                "Debe informar que el usuario no fue encontrado");
    }

    @Test
    @DisplayName("Crear un retweet lanza excepción cuando el tweet no existe")
    void crearRetweet_lanzaExcepcionCuandoElTweetNoExiste() {
        // Setup: un usuario real y un id de tweet inexistente
        // (ID_INEXISTENTE)

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> servicio.crearRetweet(idMlopez, ID_INEXISTENTE));
        assertEquals(ERROR_TWEET_NO_ENCONTRADO + ID_INEXISTENTE, excepcion.getMessage(),
                "Debe informar que el tweet no fue encontrado");
    }

    @Test
    @DisplayName("Crear un retweet lanza excepción cuando el usuario está borrado")
    void crearRetweet_lanzaExcepcionCuandoElUsuarioEstaBorrado() {
        // Setup: mlopez fue eliminado
        servicio.eliminarUsuario(idMlopez);
        Integer idTweet = DatosDePrueba.idTweet(emf, TWEET_DE_JPEREZ);

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> servicio.crearRetweet(idMlopez, idTweet));
        assertEquals(ERROR_USUARIO_NO_ENCONTRADO + idMlopez, excepcion.getMessage(),
                "Un usuario borrado no debe poder retuitear");
    }

    @Test
    @DisplayName("Crear un retweet lanza excepción cuando el tweet está borrado")
    void crearRetweet_lanzaExcepcionCuandoElTweetEstaBorrado() {
        // Setup: el tweet de jperez fue eliminado
        Integer idTweet = DatosDePrueba.idTweet(emf, TWEET_DE_JPEREZ);
        servicio.eliminarTweet(idJperez, idTweet);

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> servicio.crearRetweet(idMlopez, idTweet));
        assertEquals(ERROR_TWEET_NO_ENCONTRADO + idTweet, excepcion.getMessage(),
                "No debe poder retuitearse un tweet borrado");
    }

    @Test
    @DisplayName("Eliminar un retweet lo saca de la lista del usuario")
    void eliminarRetweet_loSacaDeLaListaDelUsuario() {
        // Setup: el retweet de agomez
        Integer idRetweet = DatosDePrueba.idRetweetDe(emf, AGOMEZ);

        // Ejercitación: su autor lo elimina
        servicio.eliminarRetweet(idAgomez, idRetweet);

        // Verificación: ya no aparece
        assertTrue(servicio.listarRetweetsDelUsuario(idAgomez).isEmpty(),
                "El retweet eliminado no debe listarse");
    }

    @Test
    @DisplayName("Eliminar un retweet lanza excepción 403 cuando quien lo pide no es el autor")
    void eliminarRetweet_lanzaExcepcionCuandoQuienPideNoEsElAutor() {
        // Setup: el retweet de agomez, que intenta borrar mlopez
        Integer idRetweet = DatosDePrueba.idRetweetDe(emf, AGOMEZ);

        // Ejercitación y Verificación
        var excepcion = assertThrows(ResponseStatusException.class,
                () -> servicio.eliminarRetweet(idMlopez, idRetweet));
        assertEquals(HttpStatus.FORBIDDEN, excepcion.getStatusCode(), "Debe responder 403");
        assertEquals(ERROR_ELIMINAR_RETWEET_AJENO, excepcion.getReason(),
                "Debe informar que no se puede eliminar un retweet ajeno");
    }

    @Test
    @DisplayName("Eliminar el retweet de otra persona no lo borra")
    void eliminarRetweet_noBorraElRetweetCuandoQuienPideNoEsElAutor() {
        // Setup: el retweet de agomez
        Integer idRetweet = DatosDePrueba.idRetweetDe(emf, AGOMEZ);

        // Ejercitación: mlopez intenta borrarlo (el pedido es rechazado)
        assertThrows(ResponseStatusException.class, () -> servicio.eliminarRetweet(idMlopez, idRetweet));

        // Verificación: el retweet sigue listado
        assertEquals(1, servicio.listarRetweetsDelUsuario(idAgomez).size(),
                "El retweet ajeno no debe borrarse");
    }

    @Test
    @DisplayName("Eliminar un retweet inexistente no falla")
    void eliminarRetweet_noFallaCuandoElRetweetNoExiste() {
        // Setup: un id que no corresponde a ningún retweet
        // (ID_INEXISTENTE)

        // Ejercitación y Verificación
        assertDoesNotThrow(() -> servicio.eliminarRetweet(idAgomez, ID_INEXISTENTE),
                "Eliminar un retweet que no existe no debe lanzar excepción");
    }

    @Test
    @DisplayName("Eliminar un retweet ignora el pedido cuando el id corresponde a un tweet original")
    void eliminarRetweet_ignoraElPedidoCuandoElIdEsDeUnTweetOriginal() {
        // Setup: el id de un tweet original de agomez
        Integer idTweet = DatosDePrueba.idTweet(emf, TWEET_DE_AGOMEZ);

        // Ejercitación: pedir eliminarlo como si fuera un retweet
        servicio.eliminarRetweet(idAgomez, idTweet);

        // Verificación: el tweet original sigue existiendo
        assertEquals(1, servicio.listarTweetsDelUsuario(idAgomez, 0).totalElements(),
                "Un tweet original no se elimina con eliminarRetweet");
    }

    // ---------- Follows ----------

    @Test
    @DisplayName("Seguir crea la relación entre dos usuarios")
    void seguir_creaLaRelacionEntreDosUsuarios() {
        // Setup: mlopez todavía no sigue a agomez
        // (escenario inicial)

        // Ejercitación: mlopez sigue a agomez
        servicio.seguir(idMlopez, idAgomez);

        // Verificación: la relación existe
        assertTrue(servicio.estaSiguiendo(idMlopez, idAgomez), "mlopez debe figurar siguiendo a agomez");
    }

    @Test
    @DisplayName("Seguir a alguien que ya se sigue no hace nada ni duplica la relación")
    void seguir_noHaceNadaCuandoYaSeSigue() {
        // Setup: agomez ya sigue a jperez en el escenario inicial
        // (escenario inicial)

        // Ejercitación y Verificación: repetir el seguimiento no falla
        assertDoesNotThrow(() -> servicio.seguir(idAgomez, idJperez),
                "Seguir de nuevo a la misma persona no debe lanzar excepción");
        assertEquals(1, servicio.obtenerSeguidores(idJperez).size(), "No debe duplicarse la relación");
    }

    @Test
    @DisplayName("Seguir lanza excepción cuando el seguidor no existe")
    void seguir_lanzaExcepcionCuandoElSeguidorNoExiste() {
        // Setup: un seguidor inexistente
        // (ID_INEXISTENTE)

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> servicio.seguir(ID_INEXISTENTE, idAgomez));
        assertEquals(ERROR_USUARIO_NO_ENCONTRADO + ID_INEXISTENTE, excepcion.getMessage(),
                "Debe informar que el usuario no fue encontrado");
    }

    @Test
    @DisplayName("Seguir lanza excepción cuando el seguido no existe")
    void seguir_lanzaExcepcionCuandoElSeguidoNoExiste() {
        // Setup: un seguido inexistente
        // (ID_INEXISTENTE)

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> servicio.seguir(idAgomez, ID_INEXISTENTE));
        assertEquals(ERROR_USUARIO_NO_ENCONTRADO + ID_INEXISTENTE, excepcion.getMessage(),
                "Debe informar que el usuario no fue encontrado");
    }

    @Test
    @DisplayName("Seguir lanza excepción cuando el seguidor está borrado")
    void seguir_lanzaExcepcionCuandoElSeguidorEstaBorrado() {
        // Setup: mlopez fue eliminado
        servicio.eliminarUsuario(idMlopez);

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> servicio.seguir(idMlopez, idAgomez));
        assertEquals(ERROR_USUARIO_NO_ENCONTRADO + idMlopez, excepcion.getMessage(),
                "Un usuario borrado no debe poder seguir a nadie");
    }

    @Test
    @DisplayName("Seguir lanza excepción cuando el seguido está borrado")
    void seguir_lanzaExcepcionCuandoElSeguidoEstaBorrado() {
        // Setup: mlopez fue eliminado
        servicio.eliminarUsuario(idMlopez);

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> servicio.seguir(idAgomez, idMlopez));
        assertEquals(ERROR_USUARIO_NO_ENCONTRADO + idMlopez, excepcion.getMessage(),
                "No debe poder seguirse a un usuario borrado");
    }

    @Test
    @DisplayName("Dejar de seguir elimina la relación")
    void dejarDeSeguir_eliminaLaRelacion() {
        // Setup: agomez sigue a jperez en el escenario inicial
        // (escenario inicial)

        // Ejercitación: agomez deja de seguir a jperez
        servicio.dejarDeSeguir(idAgomez, idJperez);

        // Verificación: la relación ya no existe
        assertFalse(servicio.estaSiguiendo(idAgomez, idJperez), "agomez ya no debe seguir a jperez");
    }

    @Test
    @DisplayName("Dejar de seguir a alguien que no se sigue no falla")
    void dejarDeSeguir_noFallaCuandoNoSeSeguian() {
        // Setup: mlopez nunca siguió a agomez
        // (escenario inicial)

        // Ejercitación y Verificación
        assertDoesNotThrow(() -> servicio.dejarDeSeguir(idMlopez, idAgomez),
                "Dejar de seguir a quien no se sigue no debe lanzar excepción");
    }

    @Test
    @DisplayName("Dejar de seguir lanza excepción cuando el seguidor no existe")
    void dejarDeSeguir_lanzaExcepcionCuandoElSeguidorNoExiste() {
        // Setup: un seguidor inexistente
        // (ID_INEXISTENTE)

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> servicio.dejarDeSeguir(ID_INEXISTENTE, idJperez));
        assertEquals(ERROR_USUARIO_NO_ENCONTRADO + ID_INEXISTENTE, excepcion.getMessage(),
                "Debe informar que el usuario no fue encontrado");
    }

    @Test
    @DisplayName("Dejar de seguir lanza excepción cuando el seguido no existe")
    void dejarDeSeguir_lanzaExcepcionCuandoElSeguidoNoExiste() {
        // Setup: un seguido inexistente
        // (ID_INEXISTENTE)

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> servicio.dejarDeSeguir(idAgomez, ID_INEXISTENTE));
        assertEquals(ERROR_USUARIO_NO_ENCONTRADO + ID_INEXISTENTE, excepcion.getMessage(),
                "Debe informar que el usuario no fue encontrado");
    }
}
