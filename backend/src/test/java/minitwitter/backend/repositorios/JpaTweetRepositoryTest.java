package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import minitwitter.backend.dto.OriginalTweetInfo;
import minitwitter.backend.dto.PageResponse;
import minitwitter.backend.dto.RetweetInfo;
import minitwitter.backend.dto.TweetInfo;
import minitwitter.backend.model.OriginalTweet;
import minitwitter.backend.model.User;
import minitwitter.backend.util.DatosDePrueba;
import minitwitter.backend.util.EmfBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static minitwitter.backend.util.DatosDePrueba.AGOMEZ;
import static minitwitter.backend.util.DatosDePrueba.ID_INEXISTENTE;
import static minitwitter.backend.util.DatosDePrueba.JPEREZ;
import static minitwitter.backend.util.DatosDePrueba.MLOPEZ;
import static minitwitter.backend.util.DatosDePrueba.TWEET_DE_AGOMEZ;
import static minitwitter.backend.util.DatosDePrueba.TWEET_DE_JPEREZ;
import static minitwitter.backend.util.DatosDePrueba.TWEET_DE_MLOPEZ;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaTweetRepositoryTest {

    private static final int TAMANIO_PEQUENIO = 2;
    private static final LocalDateTime FECHA_COMUN = LocalDateTime.of(2024, 1, 2, 10, 0);

    private static EntityManagerFactory emf;

    private EntityManager em;
    private TweetRepository repositorio;

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
        em = emf.createEntityManager();
        repositorio = TweetRepository.repositorioDe(em);
    }

    @AfterEach
    void despuesDeCadaTest() {
        em.close();
    }

    private void enTransaccion(Runnable accion) {
        em.getTransaction().begin();
        accion.run();
        em.getTransaction().commit();
    }

    // Agrega tweets de un usuario, todos con la misma fecha, para probar páginas y desempates.
    private void agregarTweetsConFechaComun(String nombreUsuario, int cantidad) {
        var autor = em.find(User.class, DatosDePrueba.idUsuario(emf, nombreUsuario));
        enTransaccion(() -> {
            for (int i = 1; i <= cantidad; i++) {
                var tweet = new OriginalTweet(autor, "Tweet repetido " + i);
                tweet.setCreatedAt(FECHA_COMUN);
                repositorio.guardar(tweet);
            }
        });
        em.clear();
    }

    private void borrarTweetDe(String contenido) {
        var tweet = em.find(OriginalTweet.class, DatosDePrueba.idTweet(emf, contenido));
        enTransaccion(() -> repositorio.eliminar(tweet));
        em.clear();
    }

    // ---------- guardar / buscarPorId / eliminar ----------

    @Test
    @DisplayName("Guardar un tweet original lo deja disponible para buscarlo por id")
    void guardar_dejaElTweetDisponibleParaBuscarloPorId() {
        // Setup: un tweet nuevo de agomez
        var agomez = em.find(User.class, DatosDePrueba.idUsuario(emf, AGOMEZ));
        var tweetNuevo = new OriginalTweet(agomez, "Un tweet nuevo");

        // Ejercitación: guardarlo
        enTransaccion(() -> repositorio.guardar(tweetNuevo));

        // Verificación: al leer de la base se encuentra con su contenido
        em.clear();
        var guardado = repositorio.buscarPorId(tweetNuevo.getId());
        assertTrue(guardado.isPresent(), "El tweet guardado debe poder encontrarse por id");
        assertEquals("Un tweet nuevo", ((OriginalTweet) guardado.get()).getContent(),
                "El contenido guardado debe ser el original");
    }

    @Test
    @DisplayName("Buscar por id devuelve vacío cuando el tweet no existe")
    void buscarPorId_devuelveVacioCuandoElTweetNoExiste() {
        // Setup: un id que no corresponde a ningún tweet
        // (ID_INEXISTENTE)

        // Ejercitación: buscarlo
        var tweet = repositorio.buscarPorId(ID_INEXISTENTE);

        // Verificación: no se encuentra nada
        assertTrue(tweet.isEmpty(), "No debe encontrar un tweet con un id inexistente");
    }

    @Test
    @DisplayName("Eliminar un tweet lo marca como borrado y conserva su contenido en la base")
    void eliminar_marcaComoBorradoYConservaElContenido() {
        // Setup: el tweet de agomez
        Integer id = DatosDePrueba.idTweet(emf, TWEET_DE_AGOMEZ);
        var tweet = em.find(OriginalTweet.class, id);

        // Ejercitación: eliminarlo
        enTransaccion(() -> repositorio.eliminar(tweet));

        // Verificación: al releer de la base está borrado pero conserva el texto
        em.clear();
        var recargado = (OriginalTweet) repositorio.buscarPorId(id).orElseThrow();
        assertTrue(recargado.isDeleted(), "El tweet debe quedar marcado como borrado");
        assertEquals(TWEET_DE_AGOMEZ, recargado.getContent(), "El contenido debe seguir guardado en la base");
    }

    // ---------- buscarPorIdAutor ----------

    @Test
    @DisplayName("Buscar por autor devuelve solo los tweets originales del autor")
    void buscarPorIdAutor_devuelveSoloLosTweetsOriginalesDelAutor() {
        // Setup: agomez tiene un tweet original y un retweet en el escenario inicial
        Integer idAgomez = DatosDePrueba.idUsuario(emf, AGOMEZ);

        // Ejercitación: listar sus tweets
        PageResponse<OriginalTweetInfo> pagina = repositorio.buscarPorIdAutor(idAgomez, 0, 10);

        // Verificación: solo aparece el original, no el retweet
        assertEquals(1, pagina.totalElements(), "Solo debe contar el tweet original");
        assertEquals(TWEET_DE_AGOMEZ, pagina.content().get(0).content(), "Debe ser el tweet original de agomez");
    }

    @Test
    @DisplayName("Buscar por autor excluye los tweets borrados")
    void buscarPorIdAutor_excluyeLosTweetsBorrados() {
        // Setup: el tweet de agomez fue borrado
        borrarTweetDe(TWEET_DE_AGOMEZ);

        // Ejercitación: listar sus tweets
        var pagina = repositorio.buscarPorIdAutor(DatosDePrueba.idUsuario(emf, AGOMEZ), 0, 10);

        // Verificación: no aparece ninguno
        assertEquals(0, pagina.totalElements(), "Un tweet borrado no debe listarse ni contarse");
    }

    @Test
    @DisplayName("Buscar por autor devuelve una página vacía cuando el autor no tiene tweets")
    void buscarPorIdAutor_devuelvePaginaVaciaCuandoElAutorNoTieneTweets() {
        // Setup: un usuario nuevo que todavía no publicó nada
        var usuarioNuevo = new User("novato01", "clave123");
        enTransaccion(() -> em.persist(usuarioNuevo));

        // Ejercitación: listar sus tweets
        var pagina = repositorio.buscarPorIdAutor(usuarioNuevo.getId(), 0, 10);

        // Verificación: la página está vacía y sin siguiente
        assertTrue(pagina.content().isEmpty(), "No debe haber tweets para listar");
        assertEquals(0, pagina.totalPages(), "Sin tweets no debe haber páginas");
        assertFalse(pagina.hasNext(), "No debe haber una página siguiente");
    }

    @Test
    @DisplayName("Buscar por autor devuelve la primera página indicando que hay siguiente")
    void buscarPorIdAutor_devuelveLaPrimeraPaginaConHaySiguiente() {
        // Setup: agomez con 5 tweets en total (1 inicial + 4 nuevos), de a 2 por página
        agregarTweetsConFechaComun(AGOMEZ, 4);

        // Ejercitación: pedir la página 0
        var pagina = repositorio.buscarPorIdAutor(DatosDePrueba.idUsuario(emf, AGOMEZ), 0, TAMANIO_PEQUENIO);

        // Verificación: trae 2 tweets y avisa que hay más
        assertEquals(TAMANIO_PEQUENIO, pagina.content().size(), "La página debe traer el tamaño pedido");
        assertEquals(5, pagina.totalElements(), "Debe contar todos los tweets del autor");
        assertEquals(3, pagina.totalPages(), "5 tweets de a 2 son 3 páginas");
        assertTrue(pagina.hasNext(), "Debe haber una página siguiente");
    }

    @Test
    @DisplayName("Buscar por autor devuelve la última página con el resto y sin siguiente")
    void buscarPorIdAutor_devuelveLaUltimaPaginaSinHaySiguiente() {
        // Setup: agomez con 5 tweets en total, de a 2 por página
        agregarTweetsConFechaComun(AGOMEZ, 4);

        // Ejercitación: pedir la última página (la 2)
        var pagina = repositorio.buscarPorIdAutor(DatosDePrueba.idUsuario(emf, AGOMEZ), 2, TAMANIO_PEQUENIO);

        // Verificación: trae el tweet que sobra y no hay más páginas
        assertEquals(1, pagina.content().size(), "La última página debe traer solo el resto");
        assertFalse(pagina.hasNext(), "No debe haber una página siguiente");
    }

    @Test
    @DisplayName("Buscar por autor ordena por fecha descendente y desempata por id descendente")
    void buscarPorIdAutor_ordenaPorFechaYDesempataPorIdDescendente() {
        // Setup: 3 tweets nuevos con exactamente la misma fecha, más recientes que el inicial
        agregarTweetsConFechaComun(AGOMEZ, 3);

        // Ejercitación: listar todos
        var pagina = repositorio.buscarPorIdAutor(DatosDePrueba.idUsuario(emf, AGOMEZ), 0, 10);

        // Verificación: los 3 nuevos van primero, del id más alto al más bajo; el viejo queda al final
        List<Integer> ids = pagina.content().stream().map(OriginalTweetInfo::id).toList();
        List<Integer> idsNuevosEsperados = new ArrayList<>(ids.subList(0, 3));
        idsNuevosEsperados.sort(Comparator.reverseOrder());
        assertEquals(idsNuevosEsperados, ids.subList(0, 3), "Con la misma fecha debe desempatar por id descendente");
        assertEquals(TWEET_DE_AGOMEZ, pagina.content().get(3).content(), "El tweet más viejo debe quedar último");
    }

    @Test
    @DisplayName("Buscar por autor devuelve una página vacía cuando la página está fuera de rango")
    void buscarPorIdAutor_devuelvePaginaVaciaCuandoLaPaginaEstaFueraDeRango() {
        // Setup: agomez tiene un solo tweet, así que la página 10 no existe
        Integer idAgomez = DatosDePrueba.idUsuario(emf, AGOMEZ);

        // Ejercitación: pedir la página 10
        var pagina = repositorio.buscarPorIdAutor(idAgomez, 10, 10);

        // Verificación: no trae tweets, pero conserva el total real
        assertTrue(pagina.content().isEmpty(), "Una página fuera de rango no debe traer tweets");
        assertEquals(1, pagina.totalElements(), "El total debe seguir siendo el real");
        assertFalse(pagina.hasNext(), "No debe haber una página siguiente");
    }

    @Test
    @DisplayName("Buscar por autor devuelve una página vacía cuando el número de página es enorme")
    void buscarPorIdAutor_devuelvePaginaVaciaCuandoLaPaginaEsEnorme() {
        // Setup: el mayor número de página posible
        Integer idAgomez = DatosDePrueba.idUsuario(emf, AGOMEZ);

        // Ejercitación: pedir esa página
        var pagina = repositorio.buscarPorIdAutor(idAgomez, Integer.MAX_VALUE, 10);

        // Verificación: no falla por desborde y devuelve una página vacía
        assertTrue(pagina.content().isEmpty(), "Una página enorme no debe traer tweets");
        assertFalse(pagina.hasNext(), "No debe haber una página siguiente");
    }

    // ---------- buscarFeedPorIdSeguidor ----------

    @Test
    @DisplayName("Buscar feed incluye tweets propios y de seguidos ordenados por fecha descendente")
    void buscarFeedPorIdSeguidor_incluyePropiosYSeguidosOrdenadosPorFecha() {
        // Setup: agomez tiene su tweet (10:00) y su retweet (12:00), y sigue a jperez (tweet 11:00)
        Integer idAgomez = DatosDePrueba.idUsuario(emf, AGOMEZ);

        // Ejercitación: pedir el feed de agomez
        List<TweetInfo> feed = repositorio.buscarFeedPorIdSeguidor(idAgomez, 0, 10).content();

        // Verificación: 3 elementos, del más nuevo al más viejo
        assertEquals(3, feed.size(), "El feed debe tener los tweets propios y los del seguido");
        assertInstanceOf(RetweetInfo.class, feed.get(0), "Primero debe estar el retweet de las 12:00");
        assertEquals(TWEET_DE_JPEREZ, ((OriginalTweetInfo) feed.get(1)).content(),
                "Segundo debe estar el tweet del seguido de las 11:00");
        assertEquals(TWEET_DE_AGOMEZ, ((OriginalTweetInfo) feed.get(2)).content(),
                "Último debe estar el tweet propio de las 10:00");
    }

    @Test
    @DisplayName("Buscar feed devuelve solo los tweets propios cuando el usuario no sigue a nadie")
    void buscarFeedPorIdSeguidor_devuelveSoloPropiosCuandoNoSigueANadie() {
        // Setup: mlopez no sigue a nadie y tiene un tweet
        Integer idMlopez = DatosDePrueba.idUsuario(emf, MLOPEZ);

        // Ejercitación: pedir el feed de mlopez
        List<TweetInfo> feed = repositorio.buscarFeedPorIdSeguidor(idMlopez, 0, 10).content();

        // Verificación: solo su tweet
        assertEquals(1, feed.size(), "El feed debe tener solo el tweet propio");
        assertEquals(TWEET_DE_MLOPEZ, ((OriginalTweetInfo) feed.get(0)).content(), "Debe ser el tweet de mlopez");
    }

    @Test
    @DisplayName("Buscar feed devuelve una página vacía cuando no hay tweets propios ni seguidos")
    void buscarFeedPorIdSeguidor_devuelveVacioCuandoNoHayTweetsNiSeguidos() {
        // Setup: un usuario nuevo sin tweets y sin seguidos
        var usuarioNuevo = new User("novato01", "clave123");
        enTransaccion(() -> em.persist(usuarioNuevo));

        // Ejercitación: pedir su feed
        var pagina = repositorio.buscarFeedPorIdSeguidor(usuarioNuevo.getId(), 0, 10);

        // Verificación: el feed está vacío
        assertTrue(pagina.content().isEmpty(), "El feed no debe tener elementos");
        assertEquals(0, pagina.totalElements(), "El total debe ser cero");
    }

    @Test
    @DisplayName("Buscar feed excluye los tweets borrados")
    void buscarFeedPorIdSeguidor_excluyeLosTweetsBorrados() {
        // Setup: el tweet de jperez (a quien sigue agomez) fue borrado
        borrarTweetDe(TWEET_DE_JPEREZ);

        // Ejercitación: pedir el feed de agomez
        List<TweetInfo> feed = repositorio.buscarFeedPorIdSeguidor(DatosDePrueba.idUsuario(emf, AGOMEZ), 0, 10).content();

        // Verificación: ya no aparece ningún tweet original de jperez
        boolean apareceTweetBorrado = feed.stream()
                .anyMatch(item -> item instanceof OriginalTweetInfo original && original.authorUsername().equals(JPEREZ));
        assertFalse(apareceTweetBorrado, "El tweet borrado no debe aparecer en el feed");
    }

    @Test
    @DisplayName("Buscar feed mantiene el retweet cuando su tweet original fue borrado")
    void buscarFeedPorIdSeguidor_mantieneElRetweetCuandoSuOrigenFueBorrado() {
        // Setup: el tweet de jperez, que agomez retuiteó, fue borrado
        borrarTweetDe(TWEET_DE_JPEREZ);

        // Ejercitación: pedir el feed de agomez
        List<TweetInfo> feed = repositorio.buscarFeedPorIdSeguidor(DatosDePrueba.idUsuario(emf, AGOMEZ), 0, 10).content();

        // Verificación: el retweet sigue en el feed, con el original marcado como borrado
        boolean retweetConOrigenBorrado = feed.stream()
                .anyMatch(item -> item instanceof RetweetInfo retweet && retweet.originalDeleted());
        assertTrue(retweetConOrigenBorrado, "El retweet debe seguir apareciendo con el original no disponible");
    }

    @Test
    @DisplayName("Buscar feed devuelve la primera página indicando que hay siguiente")
    void buscarFeedPorIdSeguidor_devuelveLaPrimeraPaginaConHaySiguiente() {
        // Setup: el feed de agomez tiene 3 elementos, de a 2 por página
        Integer idAgomez = DatosDePrueba.idUsuario(emf, AGOMEZ);

        // Ejercitación: pedir la página 0
        var pagina = repositorio.buscarFeedPorIdSeguidor(idAgomez, 0, TAMANIO_PEQUENIO);

        // Verificación: trae 2 elementos y avisa que hay más
        assertEquals(TAMANIO_PEQUENIO, pagina.content().size(), "La página debe traer el tamaño pedido");
        assertEquals(3, pagina.totalElements(), "Debe contar todos los elementos del feed");
        assertTrue(pagina.hasNext(), "Debe haber una página siguiente");
    }

    @Test
    @DisplayName("Buscar feed devuelve una página vacía cuando la página está fuera de rango")
    void buscarFeedPorIdSeguidor_devuelvePaginaVaciaCuandoLaPaginaEstaFueraDeRango() {
        // Setup: el feed de agomez tiene 3 elementos, así que la página 5 no existe
        Integer idAgomez = DatosDePrueba.idUsuario(emf, AGOMEZ);

        // Ejercitación: pedir la página 5
        var pagina = repositorio.buscarFeedPorIdSeguidor(idAgomez, 5, TAMANIO_PEQUENIO);

        // Verificación: no trae elementos, pero conserva el total real
        assertTrue(pagina.content().isEmpty(), "Una página fuera de rango no debe traer elementos");
        assertEquals(3, pagina.totalElements(), "El total debe seguir siendo el real");
        assertFalse(pagina.hasNext(), "No debe haber una página siguiente");
    }
}
