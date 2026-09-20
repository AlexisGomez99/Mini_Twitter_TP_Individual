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
    private RetweetRepository repository;

    private User agomez;
    private User jperez;
    private OriginalTweet jperezTweet;

    @BeforeAll
    static void beforeEverything() {
        emf = new EmfBuilder().h2Memory().withDropAndCreateDDL().build();
    }

    @BeforeEach
    void beforeEach() {
        // Truncate: limpia todos los datos antes de cada test, para que no queden restos del anterior.
        emf.getSchemaManager().truncate();

        em = emf.createEntityManager();
        repository = RetweetRepository.repositoryOf(em);

        agomez = new User("agomez", "password123");
        jperez = new User("jperez", "password456");
        jperezTweet = new OriginalTweet(jperez, "Tweet de Luis");

        em.getTransaction().begin();
        em.persist(agomez);
        em.persist(jperez);
        em.persist(jperezTweet);
        em.getTransaction().commit();
    }


    @Test
    void shouldSaveANewRetweet() {
        // Set up
        Retweet newRetweet = new Retweet(agomez, jperezTweet);

        // Desarrollo
        em.getTransaction().begin();
        repository.save(newRetweet);
        em.getTransaction().commit();

        // Evaluación
        assertThat(repository.findByAuthorId(agomez.getId())).hasSize(1);
    }

    @Test
    void shouldSoftDeleteARetweet() {
        // Set up
        Retweet retweet = new Retweet(agomez, jperezTweet);
        em.getTransaction().begin();
        em.persist(retweet);
        em.getTransaction().commit();

        // Desarrollo
        em.getTransaction().begin();
        repository.delete(retweet);
        em.getTransaction().commit();

        // Evaluación: la fila se conserva marcada como borrada, pero deja de listarse
        assertThat(retweet.isDeleted()).isTrue();
        assertThat(repository.findByAuthorId(agomez.getId())).isEmpty();
    }

    @Test
    void shouldStillShowARetweetAsUnavailableAfterItsOriginIsSoftDeleted() {
        // Set up
        Retweet retweet = new Retweet(agomez, jperezTweet);
        em.getTransaction().begin();
        em.persist(retweet);
        em.getTransaction().commit();

        // Desarrollo: se borra el tweet de origen (no el retweet en sí)
        em.getTransaction().begin();
        jperezTweet.markAsDeleted();
        em.getTransaction().commit();

        // Evaluación: el retweet sigue existiendo, mostrando el origen como no disponible
        var retweets = repository.findByAuthorId(agomez.getId());
        assertThat(retweets).hasSize(1);
        assertThat(retweets.get(0).originalContent()).isEqualTo("Publicación no disponible");
        assertThat(retweets.get(0).originalAuthorUsername()).isEqualTo("jperez");
    }

    @Test
    void shouldReturnOnlyTheAuthorsRetweets() {
        // Set up
        Retweet retweet = new Retweet(agomez, jperezTweet);
        em.getTransaction().begin();
        em.persist(retweet);
        em.getTransaction().commit();

        // Desarrollo
        List<RetweetInfo> retweets = repository.findByAuthorId(agomez.getId());

        // Evaluación
        assertThat(retweets).hasSize(1);
        assertThat(retweets.get(0).authorUsername()).isEqualTo("agomez");
        assertThat(retweets.get(0).originalAuthorUsername()).isEqualTo("jperez");
        assertThat(retweets.get(0).originalTweetId()).isEqualTo(jperezTweet.getId());
    }
}
