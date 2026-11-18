package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import minitwitter.backend.dto.RetweetInfo;
import minitwitter.backend.model.OriginalTweet;
import minitwitter.backend.model.Retweet;
import minitwitter.backend.model.User;
import minitwitter.backend.util.DatosDePrueba;
import minitwitter.backend.util.EmfBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static minitwitter.backend.util.DatosDePrueba.AGOMEZ;
import static minitwitter.backend.util.DatosDePrueba.MLOPEZ;
import static minitwitter.backend.util.DatosDePrueba.TWEET_DE_JPEREZ;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaRetweetRepositoryTest {

    private static EntityManagerFactory emf;

    private EntityManager em;
    private RetweetRepository repositorio;

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
        repositorio = RetweetRepository.repositorioDe(em);
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

    @Test
    @DisplayName("Guardar un retweet lo deja listado entre los retweets de su autor")
    void guardar_dejaElRetweetListadoParaSuAutor() {
        // Setup: mlopez todavía no tiene retweets y va a retuitear el tweet de jperez
        Integer idMlopez = DatosDePrueba.idUsuario(emf, MLOPEZ);
        var mlopez = em.find(User.class, idMlopez);
        var tweetDeJperez = em.find(OriginalTweet.class, DatosDePrueba.idTweet(emf, TWEET_DE_JPEREZ));

        // Ejercitación: guardar el retweet
        enTransaccion(() -> repositorio.guardar(new Retweet(mlopez, tweetDeJperez)));

        // Verificación: el retweet aparece entre los de mlopez
        em.clear();
        assertEquals(1, repositorio.buscarPorIdAutor(idMlopez).size(),
                "mlopez debe tener el retweet recién guardado");
    }

    @Test
    @DisplayName("Buscar por autor devuelve los retweets del autor con los datos del tweet original")
    void buscarPorIdAutor_devuelveLosRetweetsDelAutor() {
        // Setup: agomez tiene un retweet del tweet de jperez en el escenario inicial
        Integer idAgomez = DatosDePrueba.idUsuario(emf, AGOMEZ);

        // Ejercitación: listar los retweets de agomez
        List<RetweetInfo> retweets = repositorio.buscarPorIdAutor(idAgomez);

        // Verificación: está el retweet y apunta al tweet de jperez
        assertEquals(1, retweets.size(), "agomez debe tener un solo retweet");
        assertEquals(DatosDePrueba.idTweet(emf, TWEET_DE_JPEREZ), retweets.get(0).originalTweetId(),
                "El retweet debe apuntar al tweet de jperez");
        assertFalse(retweets.get(0).originalDeleted(), "El tweet original disponible no debe figurar como borrado");
    }

    @Test
    @DisplayName("Buscar por autor devuelve una lista vacía cuando el usuario no tiene retweets")
    void buscarPorIdAutor_devuelveVacioCuandoElAutorNoTieneRetweets() {
        // Setup: mlopez no tiene retweets en el escenario inicial
        Integer idMlopez = DatosDePrueba.idUsuario(emf, MLOPEZ);

        // Ejercitación: listar los retweets de mlopez
        List<RetweetInfo> retweets = repositorio.buscarPorIdAutor(idMlopez);

        // Verificación: no hay ninguno
        assertTrue(retweets.isEmpty(), "mlopez no debe tener retweets");
    }

    @Test
    @DisplayName("Eliminar un retweet lo saca de la lista de su autor")
    void eliminar_sacaElRetweetDeLaListaDeSuAutor() {
        // Setup: el retweet de agomez
        Integer idAgomez = DatosDePrueba.idUsuario(emf, AGOMEZ);
        var retweet = em.find(Retweet.class, DatosDePrueba.idRetweetDe(emf, AGOMEZ));

        // Ejercitación: eliminar el retweet
        enTransaccion(() -> repositorio.eliminar(retweet));

        // Verificación: ya no aparece entre los retweets de agomez
        em.clear();
        assertTrue(repositorio.buscarPorIdAutor(idAgomez).isEmpty(),
                "El retweet eliminado no debe listarse");
    }

    @Test
    @DisplayName("Buscar por autor marca el origen como no disponible cuando el tweet original fue borrado")
    void buscarPorIdAutor_marcaElOrigenNoDisponibleCuandoElTweetFueBorrado() {
        // Setup: el tweet original de jperez fue borrado
        Integer idAgomez = DatosDePrueba.idUsuario(emf, AGOMEZ);
        var tweetDeJperez = em.find(OriginalTweet.class, DatosDePrueba.idTweet(emf, TWEET_DE_JPEREZ));
        enTransaccion(tweetDeJperez::markAsDeleted);
        em.clear();

        // Ejercitación: listar los retweets de agomez
        List<RetweetInfo> retweets = repositorio.buscarPorIdAutor(idAgomez);

        // Verificación: el retweet sigue apareciendo, con el original no disponible
        assertEquals(1, retweets.size(), "El retweet debe seguir listándose");
        assertTrue(retweets.get(0).originalDeleted(), "El tweet original debe figurar como borrado");
    }
}
