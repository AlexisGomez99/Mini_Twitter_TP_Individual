package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import minitwitter.backend.dto.RetweetInfo;
import minitwitter.backend.model.OriginalTweet;
import minitwitter.backend.model.Retweet;
import minitwitter.backend.model.User;
import minitwitter.backend.util.EmfBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JpaRetweetRepositoryTest {

    private static EntityManagerFactory emf;

    private EntityManager em;
    private RetweetRepository repositorio;

    private User agomez;
    private User jperez;
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
        repositorio = RetweetRepository.repositorioDe(em);

        agomez = new User("agomez", "password123");
        jperez = new User("jperez", "password456");
        tweetJperez = new OriginalTweet(jperez, "Tweet de Luis");

        em.getTransaction().begin();
        em.persist(agomez);
        em.persist(jperez);
        em.persist(tweetJperez);
        em.getTransaction().commit();
    }


    @Test
    void deberiaGuardarUnRetweetNuevo() {
        // Set up
        Retweet retweetNuevo = new Retweet(agomez, tweetJperez);

        // Desarrollo
        em.getTransaction().begin();
        repositorio.guardar(retweetNuevo);
        em.getTransaction().commit();

        // Evaluación
        assertThat(repositorio.buscarPorIdAutor(agomez.getId())).hasSize(1);
    }

    @Test
    void deberiaEliminarLogicamenteUnRetweet() {
        // Set up
        Retweet retweet = new Retweet(agomez, tweetJperez);
        em.getTransaction().begin();
        em.persist(retweet);
        em.getTransaction().commit();

        // Desarrollo
        em.getTransaction().begin();
        repositorio.eliminar(retweet);
        em.getTransaction().commit();

        // Evaluación: la fila se conserva marcada como borrada, pero deja de listarse
        assertThat(retweet.isDeleted()).isTrue();
        assertThat(repositorio.buscarPorIdAutor(agomez.getId())).isEmpty();
    }

    @Test
    void deberiaDevolverSoloLosRetweetsDelAutor() {
        // Set up
        Retweet retweet = new Retweet(agomez, tweetJperez);
        em.getTransaction().begin();
        em.persist(retweet);
        em.getTransaction().commit();

        // Desarrollo
        List<RetweetInfo> retweets = repositorio.buscarPorIdAutor(agomez.getId());

        // Evaluación
        assertThat(retweets).hasSize(1);
        assertThat(retweets.get(0).authorUsername()).isEqualTo("agomez");
        assertThat(retweets.get(0).originalAuthorUsername()).isEqualTo("jperez");
        assertThat(retweets.get(0).originalTweetId()).isEqualTo(tweetJperez.getId());
        assertThat(retweets.get(0).originalDeleted()).isFalse();
    }
}
