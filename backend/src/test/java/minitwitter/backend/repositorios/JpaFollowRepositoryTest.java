package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import minitwitter.backend.model.User;
import minitwitter.backend.util.EmfBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JpaFollowRepositoryTest {

    private static EntityManagerFactory emf;

    private EntityManager em;
    private FollowRepository repository;

    private User agomez;
    private User jperez;

    @BeforeAll
    static void beforeEverything() {
        emf = new EmfBuilder().h2Memory().withDropAndCreateDDL().build();
    }

    @BeforeEach
    void beforeEach() {
        // Truncate: limpia todos los datos antes de cada test, para que no queden restos del anterior.
        emf.getSchemaManager().truncate();

        em = emf.createEntityManager();
        repository = FollowRepository.repositoryOf(em);

        agomez = new User("agomez", "password123");
        jperez = new User("jperez", "password456");

        em.getTransaction().begin();
        em.persist(agomez);
        em.persist(jperez);
        em.getTransaction().commit();
    }


    @Test
    void shouldCreateAFollowRelationship() {
        // Set up (usuarios cargados en beforeEach)

        // Desarrollo
        em.getTransaction().begin();
        repository.follow(agomez, jperez);
        em.getTransaction().commit();

        // Evaluación
        assertThat(repository.isFollowing(agomez.getId(), jperez.getId())).isTrue();
    }

    @Test
    void shouldRemoveAFollowRelationshipOnUnfollow() {
        // Set up
        em.getTransaction().begin();
        repository.follow(agomez, jperez);
        em.getTransaction().commit();

        // Desarrollo
        em.getTransaction().begin();
        repository.unfollow(agomez, jperez);
        em.getTransaction().commit();

        // Evaluación
        assertThat(repository.isFollowing(agomez.getId(), jperez.getId())).isFalse();
    }

    @Test
    void shouldReturnFalseWhenThereIsNoFollowRelationship() {
        // Set up (usuarios cargados en beforeEach, sin relación de seguimiento entre ellos)

        // Desarrollo
        boolean isFollowing = repository.isFollowing(agomez.getId(), jperez.getId());

        // Evaluación
        assertThat(isFollowing).isFalse();
    }

    @Test
    void shouldReturnTheFollowersOfAUser() {
        // Set up
        em.getTransaction().begin();
        repository.follow(agomez, jperez);
        em.getTransaction().commit();

        // Desarrollo
        var followers = repository.findFollowers(jperez.getId());

        // Evaluación
        assertThat(followers).hasSize(1);
        assertThat(followers.get(0).username()).isEqualTo("agomez");
    }

    @Test
    void shouldReturnTheUsersSomeoneIsFollowing() {
        // Set up
        em.getTransaction().begin();
        repository.follow(agomez, jperez);
        em.getTransaction().commit();

        // Desarrollo
        var following = repository.findFollowing(agomez.getId());

        // Evaluación
        assertThat(following).hasSize(1);
        assertThat(following.get(0).username()).isEqualTo("jperez");
    }

}
