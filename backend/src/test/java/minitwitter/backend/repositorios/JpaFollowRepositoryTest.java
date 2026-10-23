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
    private FollowRepository repositorio;

    private User agomez;
    private User jperez;

    @BeforeAll
    static void antesDeTodo() {
        emf = new EmfBuilder().h2EnMemoria().conRecreacionDeEsquema().construir();
    }

    @BeforeEach
    void antesDeCadaTest() {
        // Truncate: limpia todos los datos antes de cada test, para que no queden restos del anterior.
        emf.getSchemaManager().truncate();

        em = emf.createEntityManager();
        repositorio = FollowRepository.repositorioDe(em);

        agomez = new User("agomez", "password123");
        jperez = new User("jperez", "password456");

        em.getTransaction().begin();
        em.persist(agomez);
        em.persist(jperez);
        em.getTransaction().commit();
    }


    @Test
    void deberiaCrearUnaRelacionDeSeguimiento() {
        // Set up (usuarios cargados en beforeEach)

        // Desarrollo
        em.getTransaction().begin();
        repositorio.seguir(agomez, jperez);
        em.getTransaction().commit();

        // Evaluación
        assertThat(repositorio.estaSiguiendo(agomez.getId(), jperez.getId())).isTrue();
    }

    @Test
    void deberiaEliminarLaRelacionDeSeguimientoAlDejarDeSeguir() {
        // Set up
        em.getTransaction().begin();
        repositorio.seguir(agomez, jperez);
        em.getTransaction().commit();

        // Desarrollo
        em.getTransaction().begin();
        repositorio.dejarDeSeguir(agomez, jperez);
        em.getTransaction().commit();

        // Evaluación
        assertThat(repositorio.estaSiguiendo(agomez.getId(), jperez.getId())).isFalse();
    }

    @Test
    void deberiaDevolverFalsoCuandoNoHayRelacionDeSeguimiento() {
        // Set up (usuarios cargados en beforeEach, sin relación de seguimiento entre ellos)

        // Desarrollo
        boolean estaSiguiendo = repositorio.estaSiguiendo(agomez.getId(), jperez.getId());

        // Evaluación
        assertThat(estaSiguiendo).isFalse();
    }

    @Test
    void deberiaDevolverLosSeguidoresDeUnUsuario() {
        // Set up
        em.getTransaction().begin();
        repositorio.seguir(agomez, jperez);
        em.getTransaction().commit();

        // Desarrollo
        var seguidores = repositorio.buscarSeguidores(jperez.getId());

        // Evaluación
        assertThat(seguidores).hasSize(1);
        assertThat(seguidores.get(0).username()).isEqualTo("agomez");
    }

    @Test
    void deberiaDevolverLosUsuariosQueAlguienSigue() {
        // Set up
        em.getTransaction().begin();
        repositorio.seguir(agomez, jperez);
        em.getTransaction().commit();

        // Desarrollo
        var seguidos = repositorio.buscarSeguidos(agomez.getId());

        // Evaluación
        assertThat(seguidos).hasSize(1);
        assertThat(seguidos.get(0).username()).isEqualTo("jperez");
    }

}
