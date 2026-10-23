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
    private UserRepository repositorio;

    @BeforeAll
    static void antesDeTodo() {
        emf = new EmfBuilder().h2EnMemoria().conRecreacionDeEsquema().construir();
    }

    @BeforeEach
    void antesDeCadaTest() {
        // Truncate: limpia todos los datos antes de cada test, para que no queden restos del anterior.
        emf.getSchemaManager().truncate();

        em = emf.createEntityManager();
        repositorio = UserRepository.repositorioDe(em);

        em.getTransaction().begin();
        em.persist(new User("agomez", "password123"));
        em.persist(new User("jperez", "password456"));
        em.getTransaction().commit();
    }

    @Test
    void deberiaEncontrarUnUsuarioExistentePorNombreDeUsuario() {
        // Set up (datos cargados en beforeEach)

        // Desarrollo
        var encontrado = repositorio.obtenerPorNombreUsuario("agomez");

        // Evaluación
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getUsername()).isEqualTo("agomez");
    }

    @Test
    void deberiaDevolverVacioCuandoElNombreDeUsuarioNoExiste() {
        // Set up (datos cargados en beforeEach, ninguno se llama "no_existe")

        // Desarrollo
        var encontrado = repositorio.obtenerPorNombreUsuario("no_existe");

        // Evaluación
        assertThat(encontrado).isEmpty();
    }

    @Test
    void deberiaEncontrarUnUsuarioExistentePorId() {
        // Set up
        Integer id = repositorio.obtenerPorNombreUsuario("agomez").orElseThrow().getId();

        // Desarrollo
        var encontrado = repositorio.obtenerPorId(id);

        // Evaluación
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getUsername()).isEqualTo("agomez");
    }

    @Test
    void deberiaEncontrarUnUsuarioPorNombreDeUsuarioYContrasenia() {
        // Set up (agomez cargado en beforeEach con password "password123")

        // Desarrollo
        var encontrado = repositorio.obtenerPorNombreUsuarioYContrasenia("agomez", "password123");

        // Evaluación
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getUsername()).isEqualTo("agomez");
    }

    @Test
    void deberiaDevolverVacioCuandoLaContraseniaEsIncorrecta() {
        // Set up (agomez cargado en beforeEach)

        // Desarrollo
        var encontrado = repositorio.obtenerPorNombreUsuarioYContrasenia("agomez", "password_incorrecta");

        // Evaluación
        assertThat(encontrado).isEmpty();
    }

    @Test
    void deberiaAgregarUnUsuarioNuevo() {
        // Set up
        User usuarioNuevo = new User("nuevo_user", "clave12345");

        // Desarrollo
        em.getTransaction().begin();
        repositorio.agregarUsuario(usuarioNuevo);
        em.getTransaction().commit();

        // Evaluación
        assertThat(repositorio.obtenerPorNombreUsuario("nuevo_user")).isPresent();
    }

    @Test
    void deberiaListarTodosLosUsuarios() {
        // Set up (agomez y jperez cargados en beforeEach)

        // Desarrollo
        var usuarios = repositorio.listarUsuarios();

        // Evaluación
        assertThat(usuarios).hasSize(2);
        assertThat(usuarios).extracting("username").containsExactlyInAnyOrder("agomez", "jperez");
    }

    @Test
    void deberiaEliminarLogicamenteUnUsuarioEnCascadaConSusTweetsYFollowsPeroMantenerLosRetweetsDeOtrosComoNoDisponibles() {
        // Set up: agomez tiene un tweet, jperez lo retweetea, y se siguen mutuamente.
        // Las lecturas van en la misma transacción que las escrituras siguientes: leer
        // fuera de una transacción activa y usar ese resultado en una transacción
        // posterior hace que Hibernate no reconozca la entidad como ya persistente.
        em.getTransaction().begin();
        User agomez = repositorio.obtenerPorNombreUsuario("agomez").orElseThrow();
        User jperez = repositorio.obtenerPorNombreUsuario("jperez").orElseThrow();
        Integer idAgomez = agomez.getId();
        Integer idJperez = jperez.getId();

        OriginalTweet tweetAgomez = new OriginalTweet(agomez, "Tweet de Ana");
        Retweet retweetJperez = new Retweet(jperez, tweetAgomez);

        em.persist(tweetAgomez);
        em.persist(retweetJperez);
        FollowRepository.repositorioDe(em).seguir(agomez, jperez);
        FollowRepository.repositorioDe(em).seguir(jperez, agomez);
        em.getTransaction().commit();

        // Vacía el contexto de persistencia: agomez quedó con su colección "tweets" en
        // memoria tal cual la creó el constructor (vacía), sin saber de agomezTweet.
        // Sin este clear(), al recorrerla para marcar sus tweets como borrados, Hibernate
        // usaría esa colección desactualizada en vez de recargarla de la base.
        em.clear();

        // Desarrollo
        em.getTransaction().begin();
        User agomezABorrar = repositorio.obtenerPorNombreUsuario("agomez").orElseThrow();
        repositorio.eliminarUsuario(agomezABorrar);
        em.getTransaction().commit();

        // Evaluación: la fila de agomez se conserva, solo queda marcada como eliminada
        assertThat(repositorio.obtenerPorNombreUsuario("agomez")).isPresent();
        assertThat(repositorio.obtenerPorNombreUsuario("agomez").get().isDeleted()).isTrue();

        // Su propio tweet deja de listarse (se oculta en cascada)
        assertThat(TweetRepository.repositorioDe(em).buscarPorIdAutor(idAgomez, 0, 10).content()).isEmpty();

        // El retweet de jperez sobre ese tweet no se borra: sigue existiendo,
        // mostrando el origen como no disponible.
        var retweetsJperez = RetweetRepository.repositorioDe(em).buscarPorIdAutor(idJperez);
        assertThat(retweetsJperez).hasSize(1);
        assertThat(retweetsJperez.get(0).originalContent()).isEqualTo("Publicación no disponible");
        assertThat(retweetsJperez.get(0).originalAuthorUsername()).isEqualTo("Cuenta eliminada");

        // Las relaciones de "follow" sí se eliminan al borrar la cuenta
        assertThat(FollowRepository.repositorioDe(em).estaSiguiendo(idAgomez, idJperez)).isFalse();
        assertThat(FollowRepository.repositorioDe(em).estaSiguiendo(idJperez, idAgomez)).isFalse();
    }

    @Test
    void deberiaExcluirLosUsuariosBorradosDeListarUsuarios() {
        // Set up (agomez y jperez cargados en beforeEach)

        // Desarrollo
        em.getTransaction().begin();
        repositorio.eliminarUsuario(repositorio.obtenerPorNombreUsuario("agomez").orElseThrow());
        em.getTransaction().commit();

        // Evaluación
        assertThat(repositorio.listarUsuarios()).extracting("username").containsExactly("jperez");
    }
}
