package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import minitwitter.backend.dto.UserInfo;
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
import static minitwitter.backend.util.DatosDePrueba.JPEREZ;
import static minitwitter.backend.util.DatosDePrueba.MLOPEZ;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaFollowRepositoryTest {

    private static EntityManagerFactory emf;

    private EntityManager em;
    private FollowRepository repositorio;

    private User agomez;
    private User jperez;
    private User mlopez;

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
        repositorio = FollowRepository.repositorioDe(em);
        var usuarios = UserRepository.repositorioDe(em);
        agomez = usuarios.obtenerPorNombreUsuario(AGOMEZ).orElseThrow();
        jperez = usuarios.obtenerPorNombreUsuario(JPEREZ).orElseThrow();
        mlopez = usuarios.obtenerPorNombreUsuario(MLOPEZ).orElseThrow();
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
    @DisplayName("Seguir crea la relación entre dos usuarios")
    void seguir_creaLaRelacionEntreDosUsuarios() {
        // Setup: mlopez todavía no sigue a agomez
        // (escenario inicial)

        // Ejercitación: mlopez sigue a agomez
        enTransaccion(() -> repositorio.seguir(mlopez, agomez));

        // Verificación: la relación existe
        assertTrue(repositorio.estaSiguiendo(mlopez.getId(), agomez.getId()),
                "Debe existir la relación después de seguir");
    }

    @Test
    @DisplayName("Seguir lanza excepción cuando la relación ya existe")
    void seguir_lanzaExcepcionCuandoLaRelacionYaExiste() {
        // Setup: agomez ya sigue a jperez en el escenario inicial
        // (escenario inicial)

        // Ejercitación y Verificación: repetir la relación viola la restricción de unicidad
        assertThrows(PersistenceException.class,
                () -> enTransaccion(() -> repositorio.seguir(agomez, jperez)),
                "No debe permitir dos relaciones iguales entre los mismos usuarios");
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
    }

    @Test
    @DisplayName("Dejar de seguir elimina la relación")
    void dejarDeSeguir_eliminaLaRelacion() {
        // Setup: agomez sigue a jperez en el escenario inicial
        // (escenario inicial)

        // Ejercitación: dejar de seguirlo
        enTransaccion(() -> repositorio.dejarDeSeguir(agomez, jperez));

        // Verificación: la relación ya no existe
        assertFalse(repositorio.estaSiguiendo(agomez.getId(), jperez.getId()),
                "No debe existir la relación después de dejar de seguir");
    }

    @Test
    @DisplayName("Dejar de seguir no falla cuando la relación no existe")
    void dejarDeSeguir_noFallaCuandoLaRelacionNoExiste() {
        // Setup: mlopez nunca siguió a agomez
        // (escenario inicial)

        // Ejercitación y Verificación
        assertDoesNotThrow(() -> enTransaccion(() -> repositorio.dejarDeSeguir(mlopez, agomez)),
                "Dejar de seguir a quien no se sigue no debe fallar");
    }

    @Test
    @DisplayName("Está siguiendo devuelve true cuando existe la relación")
    void estaSiguiendo_devuelveTrueCuandoExisteLaRelacion() {
        // Setup: agomez sigue a jperez en el escenario inicial
        // (escenario inicial)

        // Ejercitación: consultar la relación
        boolean sigue = repositorio.estaSiguiendo(agomez.getId(), jperez.getId());

        // Verificación: la relación existe
        assertTrue(sigue, "agomez debe figurar siguiendo a jperez");
    }

    @Test
    @DisplayName("Está siguiendo devuelve false cuando no existe la relación")
    void estaSiguiendo_devuelveFalseCuandoNoExisteLaRelacion() {
        // Setup: mlopez no sigue a nadie en el escenario inicial
        // (escenario inicial)

        // Ejercitación: consultar la relación
        boolean sigue = repositorio.estaSiguiendo(mlopez.getId(), agomez.getId());

        // Verificación: la relación no existe
        assertFalse(sigue, "mlopez no debe figurar siguiendo a agomez");
    }

    @Test
    @DisplayName("Está siguiendo devuelve false en el sentido inverso de una relación existente")
    void estaSiguiendo_devuelveFalseEnElSentidoInverso() {
        // Setup: agomez sigue a jperez, pero jperez no sigue a agomez
        // (escenario inicial)

        // Ejercitación: consultar la relación al revés
        boolean sigue = repositorio.estaSiguiendo(jperez.getId(), agomez.getId());

        // Verificación: la relación inversa no existe
        assertFalse(sigue, "Seguir a alguien no implica que esa persona también siga de vuelta");
    }

    @Test
    @DisplayName("Buscar seguidores devuelve a quienes siguen al usuario")
    void buscarSeguidores_devuelveQuienesSiguenAlUsuario() {
        // Setup: jperez tiene como seguidor a agomez en el escenario inicial
        // (escenario inicial)

        // Ejercitación: buscar los seguidores de jperez
        List<String> seguidores = repositorio.buscarSeguidores(jperez.getId()).stream().map(UserInfo::username).toList();

        // Verificación: solo está agomez
        assertEquals(List.of(AGOMEZ), seguidores, "Los seguidores de jperez deben ser solo agomez");
    }

    @Test
    @DisplayName("Buscar seguidores devuelve una lista vacía cuando nadie sigue al usuario")
    void buscarSeguidores_devuelveVacioCuandoNadieSigueAlUsuario() {
        // Setup: nadie sigue a agomez en el escenario inicial
        // (escenario inicial)

        // Ejercitación: buscar los seguidores de agomez
        List<UserInfo> seguidores = repositorio.buscarSeguidores(agomez.getId());

        // Verificación: no hay seguidores
        assertTrue(seguidores.isEmpty(), "agomez no debe tener seguidores");
    }

    @Test
    @DisplayName("Buscar seguidos devuelve a quienes sigue el usuario")
    void buscarSeguidos_devuelveAQuienesSigueElUsuario() {
        // Setup: agomez sigue a jperez en el escenario inicial
        // (escenario inicial)

        // Ejercitación: buscar a quienes sigue agomez
        List<String> seguidos = repositorio.buscarSeguidos(agomez.getId()).stream().map(UserInfo::username).toList();

        // Verificación: solo está jperez
        assertEquals(List.of(JPEREZ), seguidos, "agomez debe seguir solo a jperez");
    }

    @Test
    @DisplayName("Buscar seguidos devuelve una lista vacía cuando el usuario no sigue a nadie")
    void buscarSeguidos_devuelveVacioCuandoNoSigueANadie() {
        // Setup: jperez no sigue a nadie en el escenario inicial
        // (escenario inicial)

        // Ejercitación: buscar a quienes sigue jperez
        List<UserInfo> seguidos = repositorio.buscarSeguidos(jperez.getId());

        // Verificación: no sigue a nadie
        assertTrue(seguidos.isEmpty(), "jperez no debe seguir a nadie");
    }
}
