package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import minitwitter.backend.dto.OriginalTweetInfo;
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
    private TweetRepository repository;

    private User agomez;
    private User jperez;
    private OriginalTweet agomezTweet;
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
        repository = TweetRepository.repositoryOf(em);

        agomez = new User("agomez", "password123");
        jperez = new User("jperez", "password456");

        agomezTweet = new OriginalTweet(agomez, "Primer tweet de Ana");
        agomezTweet.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));

        jperezTweet = new OriginalTweet(jperez, "Tweet de Luis");
        jperezTweet.setCreatedAt(LocalDateTime.of(2024, 1, 1, 11, 0));

        // Ana retweetea el tweet de Luis, más tarde que ambos originales.
        Retweet agomezRetweet = new Retweet(agomez, jperezTweet);
        agomezRetweet.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0));

        em.getTransaction().begin();
        em.persist(agomez);
        em.persist(jperez);
        em.persist(agomezTweet);
        em.persist(jperezTweet);
        em.persist(agomezRetweet);
        em.getTransaction().commit();
    }


    @Test
    void shouldSaveANewOriginalTweet() {
        // Set up
        OriginalTweet newTweet = new OriginalTweet(agomez, "Un tweet nuevo");

        // Desarrollo
        em.getTransaction().begin();
        repository.save(newTweet);
        em.getTransaction().commit();

        // Evaluación
        assertThat(repository.findById(newTweet.getId())).isPresent();
    }

    @Test
    void shouldFindATweetById() {
        // Set up (datos cargados en beforeEach)

        // Desarrollo
        var found = repository.findById(agomezTweet.getId());

        // Evaluación
        assertThat(found).isPresent();
        assertThat(found.get()).isInstanceOf(OriginalTweet.class);
    }

    @Test
    void shouldReturnEmptyWhenTweetIdDoesNotExist() {
        // Set up (datos cargados en beforeEach, ningún tweet tiene id 999999)

        // Desarrollo
        var found = repository.findById(999999);

        // Evaluación
        assertThat(found).isEmpty();
    }

    @Test
    void shouldSoftDeleteATweetButKeepItsRetweetsShowingItAsUnavailable() {
        // Set up: jperezTweet ya tiene un retweet de agomez (cargado en beforeEach)

        // Desarrollo
        em.getTransaction().begin();
        repository.delete(jperezTweet);
        em.getTransaction().commit();

        // Evaluación: el tweet original se conserva, solo queda marcado como borrado
        var found = repository.findById(jperezTweet.getId());
        assertThat(found).isPresent();
        assertThat(found.get().isDeleted()).isTrue();

        // El retweet de agomez sigue existiendo, mostrando el origen como no disponible
        var agomezRetweets = RetweetRepository.repositoryOf(em).findByAuthorId(agomez.getId());
        assertThat(agomezRetweets).hasSize(1);
        assertThat(agomezRetweets.get(0).originalContent()).isEqualTo("Publicación no disponible");
        assertThat(agomezRetweets.get(0).originalAuthorUsername()).isEqualTo("jperez");
    }

    @Test
    void shouldExcludeADeletedTweetFromTheAuthorsOriginalTweetList() {
        // Set up: agomezTweet ya existe (cargado en beforeEach)

        // Desarrollo
        em.getTransaction().begin();
        repository.delete(agomezTweet);
        em.getTransaction().commit();

        // Evaluación
        assertThat(repository.findByAuthorId(agomez.getId())).isEmpty();
    }

    @Test
    void shouldExcludeADeletedTweetFromTheTimeline() {
        // Set up: agomezTweet y agomezRetweet ya existen (cargados en beforeEach)

        // Desarrollo
        em.getTransaction().begin();
        repository.delete(agomezTweet);
        em.getTransaction().commit();

        // Evaluación: en el timeline de agomez solo queda el retweet
        assertThat(repository.findTimelineByAuthorId(agomez.getId())).hasSize(1);
    }

    @Test
    void shouldReturnOnlyTheAuthorsOriginalTweets() {
        // Set up (datos cargados en beforeEach: agomez tiene 1 original y 1 retweet)

        // Desarrollo
        List<OriginalTweetInfo> originalTweets = repository.findByAuthorId(agomez.getId());

        // Evaluación
        assertThat(originalTweets).hasSize(1);
        assertThat(originalTweets.get(0).content()).isEqualTo("Primer tweet de Ana");
        assertThat(originalTweets.get(0).authorUsername()).isEqualTo("agomez");
    }

    @Test
    void shouldReturnTheMixedTimelineOrderedByDateDescending() {
        // Set up (datos cargados en beforeEach: retweet a las 12:00, original a las 10:00)

        // Desarrollo
        List<TweetInfo> timeline = repository.findTimelineByAuthorId(agomez.getId());

        // Evaluación
        assertThat(timeline).hasSize(2);
        assertThat(timeline.get(0).createdAt()).isAfter(timeline.get(1).createdAt());
    }
}
