package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import minitwitter.backend.model.OriginalTweet;
import minitwitter.backend.model.Retweet;
import minitwitter.backend.model.User;
import minitwitter.backend.util.EmfBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JpaUserRepositoryTest {

    private static EntityManagerFactory emf;

    private EntityManager em;
    private UserRepository repository;

    @BeforeAll
    static void beforeEverything() {
        emf = new EmfBuilder().h2Memory().withDropAndCreateDDL().build();
    }

    @BeforeEach
    void beforeEach() {
        // Truncate: limpia todos los datos antes de cada test, para que no queden restos del anterior.
        emf.getSchemaManager().truncate();

        em = emf.createEntityManager();
        repository = UserRepository.repositoryOf(em);

        em.getTransaction().begin();
        em.persist(new User("agomez", "password123"));
        em.persist(new User("jperez", "password456"));
        em.getTransaction().commit();
    }

    @Test
    void shouldFindAnExistingUserByUsername() {
        // Set up (datos cargados en beforeEach)

        // Desarrollo
        var found = repository.getForUsername("agomez");

        // Evaluación
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("agomez");
    }

    @Test
    void shouldReturnEmptyWhenTheUsernameDoesNotExist() {
        // Set up (datos cargados en beforeEach, ninguno se llama "no_existe")

        // Desarrollo
        var found = repository.getForUsername("no_existe");

        // Evaluación
        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindAnExistingUserById() {
        // Set up
        Integer id = repository.getForUsername("agomez").orElseThrow().getId();

        // Desarrollo
        var found = repository.getById(id);

        // Evaluación
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("agomez");
    }

    @Test
    void shouldReturnEmptyWhenTheIdDoesNotExist() {
        // Set up (datos cargados en beforeEach, ningún usuario tiene id 999999)

        // Desarrollo
        var found = repository.getById(999999);

        // Evaluación
        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindAUserByUsernameAndPassword() {
        // Set up (agomez cargado en beforeEach con password "password123")

        // Desarrollo
        var found = repository.fetchForUsernameAndPassword("agomez", "password123");

        // Evaluación
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("agomez");
    }

    @Test
    void shouldReturnEmptyWhenThePasswordIsIncorrect() {
        // Set up (agomez cargado en beforeEach)

        // Desarrollo
        var found = repository.fetchForUsernameAndPassword("agomez", "password_incorrecta");

        // Evaluación
        assertThat(found).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenFetchingByUsernameAndPasswordForANonexistentUsername() {
        // Set up (datos cargados en beforeEach, ninguno se llama "no_existe")

        // Desarrollo
        var found = repository.fetchForUsernameAndPassword("no_existe", "cualquier_clave");

        // Evaluación
        assertThat(found).isEmpty();
    }

    @Test
    void shouldAddANewUser() {
        // Set up
        User newUser = new User("nuevo_user", "clave12345");

        // Desarrollo
        em.getTransaction().begin();
        repository.addUser(newUser);
        em.getTransaction().commit();

        // Evaluación
        assertThat(repository.getForUsername("nuevo_user")).isPresent();
    }

    @Test
    void shouldListAllUsers() {
        // Set up (agomez y jperez cargados en beforeEach)

        // Desarrollo
        var users = repository.listUsers();

        // Evaluación
        assertThat(users).hasSize(2);
        assertThat(users).extracting("username").containsExactlyInAnyOrder("agomez", "jperez");
    }

    @Test
    void shouldSoftDeleteAUserCascadingItsTweetsAndFollowsButKeepingOthersRetweetsAsUnavailable() {
        // Set up: agomez tiene un tweet, jperez lo retweetea, y se siguen mutuamente.
        // Las lecturas van en la misma transacción que las escrituras siguientes: leer
        // fuera de una transacción activa y usar ese resultado en una transacción
        // posterior hace que Hibernate no reconozca la entidad como ya persistente.
        em.getTransaction().begin();
        User agomez = repository.getForUsername("agomez").orElseThrow();
        User jperez = repository.getForUsername("jperez").orElseThrow();
        Integer agomezId = agomez.getId();
        Integer jperezId = jperez.getId();

        OriginalTweet agomezTweet = new OriginalTweet(agomez, "Tweet de Ana");
        Retweet jperezRetweet = new Retweet(jperez, agomezTweet);

        em.persist(agomezTweet);
        em.persist(jperezRetweet);
        FollowRepository.repositoryOf(em).follow(agomez, jperez);
        FollowRepository.repositoryOf(em).follow(jperez, agomez);
        em.getTransaction().commit();

        // Vacía el contexto de persistencia: agomez quedó con su colección "tweets" en
        // memoria tal cual la creó el constructor (vacía), sin saber de agomezTweet.
        // Sin este clear(), al recorrerla para marcar sus tweets como borrados, Hibernate
        // usaría esa colección desactualizada en vez de recargarla de la base.
        em.clear();

        // Desarrollo
        em.getTransaction().begin();
        User agomezToDelete = repository.getForUsername("agomez").orElseThrow();
        repository.deleteUser(agomezToDelete);
        em.getTransaction().commit();

        // Evaluación: la fila de agomez se conserva, solo queda marcada como eliminada
        assertThat(repository.getForUsername("agomez")).isPresent();
        assertThat(repository.getForUsername("agomez").get().isDeleted()).isTrue();

        // Su propio tweet deja de listarse (se oculta en cascada)
        assertThat(TweetRepository.repositoryOf(em).findByAuthorId(agomezId)).isEmpty();

        // El retweet de jperez sobre ese tweet no se borra: sigue existiendo,
        // mostrando el origen como no disponible.
        var jperezRetweets = RetweetRepository.repositoryOf(em).findByAuthorId(jperezId);
        assertThat(jperezRetweets).hasSize(1);
        assertThat(jperezRetweets.get(0).originalContent()).isEqualTo("Publicación no disponible");
        assertThat(jperezRetweets.get(0).originalAuthorUsername()).isEqualTo("Cuenta eliminada");

        // Las relaciones de "follow" sí se eliminan al borrar la cuenta
        assertThat(FollowRepository.repositoryOf(em).isFollowing(agomezId, jperezId)).isFalse();
        assertThat(FollowRepository.repositoryOf(em).isFollowing(jperezId, agomezId)).isFalse();
    }

    @Test
    void shouldExcludeDeletedUsersFromListUsers() {
        // Set up (agomez y jperez cargados en beforeEach)

        // Desarrollo
        em.getTransaction().begin();
        repository.deleteUser(repository.getForUsername("agomez").orElseThrow());
        em.getTransaction().commit();

        // Evaluación
        assertThat(repository.listUsers()).extracting("username").containsExactly("jperez");
    }
}
