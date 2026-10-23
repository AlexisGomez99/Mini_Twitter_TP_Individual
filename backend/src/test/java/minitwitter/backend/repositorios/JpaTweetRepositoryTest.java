package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import minitwitter.backend.dto.OriginalTweetInfo;
import minitwitter.backend.dto.RetweetInfo;
import minitwitter.backend.dto.TweetInfo;
import minitwitter.backend.model.OriginalTweet;
import minitwitter.backend.model.Retweet;
import minitwitter.backend.model.Tweet;
import minitwitter.backend.model.User;
import minitwitter.backend.util.EmfBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JpaTweetRepositoryTest {

    private static EntityManagerFactory emf;

    private EntityManager em;
    private TweetRepository repositorio;

    private User agomez;
    private User jperez;
    private OriginalTweet tweetAgomez;
    private OriginalTweet tweetJperez;

    @BeforeAll
    static void antesDeTodo() {
        emf = new EmfBuilder().h2EnMemoria().conRecreacionDeEsquema().construir();
    }

    @BeforeEach
    void antesDeCadaTest() {
        // Truncate: limpia todos los datos antes de cada test, para que no queden restos del anterior.
        emf.getSchemaManager().truncate();

        em = emf.createEntityManager();
        repositorio = TweetRepository.repositorioDe(em);

        agomez = new User("agomez", "password123");
        jperez = new User("jperez", "password456");

        tweetAgomez = new OriginalTweet(agomez, "Primer tweet de Ana");
        tweetAgomez.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));

        tweetJperez = new OriginalTweet(jperez, "Tweet de Luis");
        tweetJperez.setCreatedAt(LocalDateTime.of(2024, 1, 1, 11, 0));

        // Ana retweetea el tweet de Luis, más tarde que ambos originales.
        Retweet retweetAgomez = new Retweet(agomez, tweetJperez);
        retweetAgomez.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0));

        em.getTransaction().begin();
        em.persist(agomez);
        em.persist(jperez);
        em.persist(tweetAgomez);
        em.persist(tweetJperez);
        em.persist(retweetAgomez);
        em.getTransaction().commit();
    }


    @Test
    void deberiaGuardarUnTweetOriginalNuevo() {
        // Set up
        OriginalTweet tweetNuevo = new OriginalTweet(agomez, "Un tweet nuevo");

        // Desarrollo
        em.getTransaction().begin();
        repositorio.guardar(tweetNuevo);
        em.getTransaction().commit();

        // Evaluación
        assertThat(repositorio.buscarPorId(tweetNuevo.getId())).isPresent();
    }

    @Test
    void deberiaEncontrarUnTweetPorId() {
        // Set up (datos cargados en beforeEach)

        // Desarrollo
        var encontrado = repositorio.buscarPorId(tweetAgomez.getId());

        // Evaluación
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get()).isInstanceOf(OriginalTweet.class);
    }

    @Test
    void deberiaEliminarLogicamenteUnTweetPeroMantenerSusRetweetsMostrandoloComoNoDisponible() {
        // Set up: jperezTweet ya tiene un retweet de agomez (cargado en beforeEach)

        // Desarrollo
        em.getTransaction().begin();
        repositorio.eliminar(tweetJperez);
        em.getTransaction().commit();

        // Evaluación: el tweet original se conserva, solo queda marcado como borrado
        var encontrado = repositorio.buscarPorId(tweetJperez.getId());
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().isDeleted()).isTrue();

        // El retweet de agomez sigue existiendo, mostrando el origen como no disponible
        var retweetsAgomez = RetweetRepository.repositorioDe(em).buscarPorIdAutor(agomez.getId());
        assertThat(retweetsAgomez).hasSize(1);
        assertThat(retweetsAgomez.get(0).originalContent()).isEqualTo("Publicación no disponible");
        assertThat(retweetsAgomez.get(0).originalAuthorUsername()).isEqualTo("jperez");
    }

    @Test
    void deberiaConservarElContenidoDeUnTweetEliminadoLogicamenteEnLaBaseDeDatos() {
        // Set up
        Integer id = tweetAgomez.getId();
        em.getTransaction().begin();
        repositorio.eliminar(tweetAgomez);
        em.getTransaction().commit();

        // Desarrollo: se vacía el contexto de persistencia para releer la fila desde la base
        em.clear();
        var recargado = (OriginalTweet) repositorio.buscarPorId(id).orElseThrow();

        // Evaluación: la fila quedó marcada como borrada pero conserva su texto
        assertThat(recargado.isDeleted()).isTrue();
        assertThat(recargado.getContent()).isEqualTo("Primer tweet de Ana");
    }

    @Test
    void deberiaExcluirUnTweetBorradoDeLaListaDeTweetsOriginalesDelAutor() {
        // Set up: agomezTweet ya existe (cargado en beforeEach)

        // Desarrollo
        em.getTransaction().begin();
        repositorio.eliminar(tweetAgomez);
        em.getTransaction().commit();

        // Evaluación
        assertThat(repositorio.buscarPorIdAutor(agomez.getId(), 0, 10).content()).isEmpty();
    }

    @Test
    void deberiaDevolverSoloLosTweetsOriginalesDelAutor() {
        // Set up (datos cargados en beforeEach: agomez tiene 1 original y 1 retweet)

        // Desarrollo
        List<OriginalTweetInfo> tweetsOriginales = repositorio.buscarPorIdAutor(agomez.getId(), 0, 10).content();

        // Evaluación
        assertThat(tweetsOriginales).hasSize(1);
        assertThat(tweetsOriginales.get(0).content()).isEqualTo("Primer tweet de Ana");
        assertThat(tweetsOriginales.get(0).authorUsername()).isEqualTo("agomez");
    }

    @Test
    void deberiaDevolverElFeedConLosTweetsPropiosYLosDeLosUsuariosSeguidos() {
        // Set up: agomez sigue a jperez. Tiene un original propio (10:00) y un retweet
        // propio (12:00); jperez tiene un original (11:00). Todos cargados en beforeEach.
        seguir(agomez, jperez);

        // Desarrollo
        List<TweetInfo> feed = repositorio.buscarFeedPorIdSeguidor(agomez.getId(), 0, 20).content();

        // Evaluación: los propios y los del seguido, del más nuevo al más viejo
        assertThat(feed).hasSize(3);
        assertThat(feed.get(0)).isInstanceOf(RetweetInfo.class);
        assertThat(((OriginalTweetInfo) feed.get(1)).content()).isEqualTo("Tweet de Luis");
        assertThat(((OriginalTweetInfo) feed.get(2)).content()).isEqualTo("Primer tweet de Ana");
    }

    @Test
    void deberiaDevolverSoloLosTweetsPropiosCuandoElUsuarioNoSigueANadie() {
        // Set up (datos cargados en beforeEach, sin ningún follow)

        // Desarrollo
        List<TweetInfo> feed = repositorio.buscarFeedPorIdSeguidor(agomez.getId(), 0, 20).content();

        // Evaluación: solo el original y el retweet propios de agomez
        assertThat(feed).hasSize(2);
    }

    @Test
    void deberiaDevolverElFeedOrdenadoPorFechaDescendenteIncluyendoRetweets() {
        // Set up: jperez sigue a agomez, que tiene un original (10:00) y un retweet (12:00);
        // jperez tiene un original propio (11:00)
        seguir(jperez, agomez);

        // Desarrollo
        List<TweetInfo> feed = repositorio.buscarFeedPorIdSeguidor(jperez.getId(), 0, 20).content();

        // Evaluación
        assertThat(feed).hasSize(3);
        assertThat(feed.get(0)).isInstanceOf(RetweetInfo.class);
        assertThat(((OriginalTweetInfo) feed.get(1)).authorUsername()).isEqualTo("jperez");
        assertThat(((OriginalTweetInfo) feed.get(2)).authorUsername()).isEqualTo("agomez");
    }

    @Test
    void deberiaExcluirLosTweetsBorradosDelFeed() {
        // Set up
        seguir(agomez, jperez);

        // Desarrollo
        em.getTransaction().begin();
        repositorio.eliminar(tweetJperez);
        em.getTransaction().commit();

        // Evaluación: el tweet borrado no aparece. Quedan el original propio de agomez y su
        // retweet (este último con el origen como no disponible).
        List<TweetInfo> feed = repositorio.buscarFeedPorIdSeguidor(agomez.getId(), 0, 20).content();
        assertThat(feed).hasSize(2);
        assertThat(feed).noneMatch(t -> t instanceof OriginalTweetInfo original
                && original.authorUsername().equals("jperez"));
    }

    @Test
    void deberiaPaginarLosTweetsDelAutorSinRepetirNiSaltearNingunoAunConLaMismaFecha() {
        // Set up: agomez ya tiene 1 original (10:00); se suman 4 con exactamente la misma fecha
        LocalDateTime mismaFecha = LocalDateTime.of(2024, 1, 2, 10, 0);
        em.getTransaction().begin();
        for (int i = 1; i <= 4; i++) {
            OriginalTweet tweet = new OriginalTweet(agomez, "Mismo horario " + i);
            tweet.setCreatedAt(mismaFecha);
            em.persist(tweet);
        }
        em.getTransaction().commit();

        // Desarrollo: 5 tweets de a 2 por página
        var primeraPagina = repositorio.buscarPorIdAutor(agomez.getId(), 0, 2);
        var segundaPagina = repositorio.buscarPorIdAutor(agomez.getId(), 1, 2);
        var ultimaPagina = repositorio.buscarPorIdAutor(agomez.getId(), 2, 2);

        // Evaluación: 3 páginas (2, 2 y 1), ningún tweet repetido ni salteado
        assertThat(primeraPagina.content()).hasSize(2);
        assertThat(segundaPagina.content()).hasSize(2);
        assertThat(ultimaPagina.content()).hasSize(1);
        assertThat(primeraPagina.totalElements()).isEqualTo(5);
        assertThat(primeraPagina.totalPages()).isEqualTo(3);
        assertThat(primeraPagina.hasNext()).isTrue();
        assertThat(ultimaPagina.hasNext()).isFalse();
        var todosLosIds = List.of(primeraPagina, segundaPagina, ultimaPagina).stream()
                .flatMap(pagina -> pagina.content().stream()).map(OriginalTweetInfo::id).toList();
        assertThat(todosLosIds).doesNotHaveDuplicates().hasSize(5);
        // El más viejo (10:00) queda último
        assertThat(ultimaPagina.content().get(0).content()).isEqualTo("Primer tweet de Ana");
    }

    @Test
    void deberiaPaginarElFeedYDevolverUnaPaginaVaciaCuandoLaPaginaEstaFueraDeRango() {
        // Set up: agomez sigue a jperez; su feed tiene 3 elementos (su original, su retweet
        // y el original de jperez)
        seguir(agomez, jperez);

        // Desarrollo
        var primeraPagina = repositorio.buscarFeedPorIdSeguidor(agomez.getId(), 0, 2);
        var segundaPagina = repositorio.buscarFeedPorIdSeguidor(agomez.getId(), 1, 2);
        var fueraDeRango = repositorio.buscarFeedPorIdSeguidor(agomez.getId(), 5, 2);

        // Evaluación
        assertThat(primeraPagina.content()).hasSize(2);
        assertThat(primeraPagina.hasNext()).isTrue();
        assertThat(segundaPagina.content()).hasSize(1);
        assertThat(segundaPagina.hasNext()).isFalse();
        assertThat(fueraDeRango.content()).isEmpty();
        assertThat(fueraDeRango.totalElements()).isEqualTo(3);
        assertThat(fueraDeRango.hasNext()).isFalse();

        // Una página enorme tampoco rompe nada: queda vacía y sin página siguiente
        var paginaEnorme = repositorio.buscarFeedPorIdSeguidor(agomez.getId(), Integer.MAX_VALUE, 2);
        assertThat(paginaEnorme.content()).isEmpty();
        assertThat(paginaEnorme.hasNext()).isFalse();
    }

    private void seguir(User seguidor, User seguido) {
        em.getTransaction().begin();
        FollowRepository.repositorioDe(em).seguir(seguidor, seguido);
        em.getTransaction().commit();
    }
}
