package minitwitter.backend.repositorios;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import minitwitter.backend.dto.UserInfo;
import minitwitter.backend.model.Tweet;
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
import static minitwitter.backend.util.DatosDePrueba.CONTRASENIA_AGOMEZ;
import static minitwitter.backend.util.DatosDePrueba.ID_INEXISTENTE;
import static minitwitter.backend.util.DatosDePrueba.JPEREZ;
import static minitwitter.backend.util.DatosDePrueba.MLOPEZ;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaUserRepositoryTest {

    private static EntityManagerFactory emf;

    private EntityManager em;
    private UserRepository repositorio;

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
        repositorio = UserRepository.repositorioDe(em);
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
    @DisplayName("Obtener por nombre de usuario devuelve el usuario cuando existe")
    void obtenerPorNombreUsuario_devuelveElUsuarioCuandoExiste() {
        // Setup: el escenario inicial tiene al usuario agomez
        // (cargado en el antesDeCadaTest)

        // Ejercitación: buscarlo por su nombre
        var usuario = repositorio.obtenerPorNombreUsuario(AGOMEZ);

        // Verificación: se encuentra el usuario con ese nombre
        assertTrue(usuario.isPresent(), "Debe encontrar al usuario existente");
        assertEquals(AGOMEZ, usuario.get().getUsername(), "El usuario encontrado debe tener el nombre buscado");
    }

    @Test
    @DisplayName("Obtener por nombre de usuario devuelve vacío cuando no existe")
    void obtenerPorNombreUsuario_devuelveVacioCuandoNoExiste() {
        // Setup: un nombre que no está en el escenario inicial
        String nombreInexistente = "no_existe";

        // Ejercitación: buscarlo
        var usuario = repositorio.obtenerPorNombreUsuario(nombreInexistente);

        // Verificación: no se encuentra nada
        assertTrue(usuario.isEmpty(), "No debe encontrar un usuario que no existe");
    }

    @Test
    @DisplayName("Obtener por id devuelve el usuario cuando existe")
    void obtenerPorId_devuelveElUsuarioCuandoExiste() {
        // Setup: el id de un usuario existente
        Integer id = DatosDePrueba.idUsuario(emf, AGOMEZ);

        // Ejercitación: buscarlo por id
        var usuario = repositorio.obtenerPorId(id);

        // Verificación: se encuentra el usuario con ese id
        assertTrue(usuario.isPresent(), "Debe encontrar al usuario existente");
        assertEquals(AGOMEZ, usuario.get().getUsername(), "El usuario encontrado debe ser el del id buscado");
    }

    @Test
    @DisplayName("Obtener por id devuelve vacío cuando no existe")
    void obtenerPorId_devuelveVacioCuandoNoExiste() {
        // Setup: un id que no corresponde a ningún usuario
        // (ID_INEXISTENTE)

        // Ejercitación: buscarlo
        var usuario = repositorio.obtenerPorId(ID_INEXISTENTE);

        // Verificación: no se encuentra nada
        assertTrue(usuario.isEmpty(), "No debe encontrar un usuario con un id inexistente");
    }

    @Test
    @DisplayName("Obtener por nombre y contraseña devuelve el usuario cuando coinciden")
    void obtenerPorNombreUsuarioYContrasenia_devuelveElUsuarioCuandoCoinciden() {
        // Setup: credenciales correctas de un usuario existente
        // (AGOMEZ y su contraseña)

        // Ejercitación: buscar con ambas credenciales
        var usuario = repositorio.obtenerPorNombreUsuarioYContrasenia(AGOMEZ, CONTRASENIA_AGOMEZ);

        // Verificación: se encuentra el usuario
        assertTrue(usuario.isPresent(), "Debe encontrar al usuario cuando nombre y contraseña coinciden");
    }

    @Test
    @DisplayName("Obtener por nombre y contraseña devuelve vacío cuando la contraseña es incorrecta")
    void obtenerPorNombreUsuarioYContrasenia_devuelveVacioCuandoLaContraseniaEsIncorrecta() {
        // Setup: el nombre de un usuario existente con una contraseña equivocada
        String contraseniaIncorrecta = "otra_clave";

        // Ejercitación: buscar con esas credenciales
        var usuario = repositorio.obtenerPorNombreUsuarioYContrasenia(AGOMEZ, contraseniaIncorrecta);

        // Verificación: no se encuentra nada
        assertTrue(usuario.isEmpty(), "No debe encontrar al usuario si la contraseña no coincide");
    }

    @Test
    @DisplayName("Agregar un usuario lo guarda en la base")
    void agregarUsuario_guardaElUsuarioEnLaBase() {
        // Setup: un usuario nuevo que no está en el escenario inicial
        var usuarioNuevo = new User("lgomez", "clave123");

        // Ejercitación: guardarlo
        enTransaccion(() -> repositorio.agregarUsuario(usuarioNuevo));

        // Verificación: al leer de la base se encuentra
        em.clear();
        assertTrue(repositorio.obtenerPorNombreUsuario("lgomez").isPresent(),
                "El usuario agregado debe poder encontrarse después de guardarlo");
    }

    @Test
    @DisplayName("Listar usuarios devuelve todos ordenados por nombre")
    void listarUsuarios_devuelveTodosOrdenadosPorNombre() {
        // Setup: el escenario inicial tiene tres usuarios
        // (cargados en el antesDeCadaTest)

        // Ejercitación: listar los usuarios
        List<String> nombres = repositorio.listarUsuarios().stream().map(UserInfo::username).toList();

        // Verificación: están los tres, en orden alfabético
        assertEquals(List.of(AGOMEZ, JPEREZ, MLOPEZ), nombres, "Debe listar todos los usuarios ordenados por nombre");
    }

    @Test
    @DisplayName("Listar usuarios excluye los usuarios borrados")
    void listarUsuarios_excluyeLosUsuariosBorrados() {
        // Setup: un usuario borrado
        var mlopez = repositorio.obtenerPorNombreUsuario(MLOPEZ).orElseThrow();
        enTransaccion(() -> repositorio.eliminarUsuario(mlopez));

        // Ejercitación: listar los usuarios
        List<String> nombres = repositorio.listarUsuarios().stream().map(UserInfo::username).toList();

        // Verificación: el borrado no aparece
        assertEquals(List.of(AGOMEZ, JPEREZ), nombres, "No debe listar a los usuarios borrados");
    }

    @Test
    @DisplayName("Eliminar un usuario lo marca como borrado sin borrar su fila")
    void eliminarUsuario_marcaComoBorradoSinBorrarLaFila() {
        // Setup: un usuario existente
        var mlopez = repositorio.obtenerPorNombreUsuario(MLOPEZ).orElseThrow();
        Integer id = mlopez.getId();

        // Ejercitación: eliminarlo
        enTransaccion(() -> repositorio.eliminarUsuario(mlopez));

        // Verificación: la fila sigue en la base, marcada como borrada
        em.clear();
        var recargado = repositorio.obtenerPorId(id);
        assertTrue(recargado.isPresent(), "La fila del usuario debe seguir existiendo");
        assertTrue(recargado.get().isDeleted(), "El usuario debe quedar marcado como borrado");
    }

    @Test
    @DisplayName("Eliminar un usuario oculta sus tweets")
    void eliminarUsuario_ocultaSusTweets() {
        // Setup: un usuario que tiene un tweet publicado
        var jperez = repositorio.obtenerPorNombreUsuario(JPEREZ).orElseThrow();
        Integer idJperez = jperez.getId();

        // Ejercitación: eliminar al usuario
        enTransaccion(() -> repositorio.eliminarUsuario(jperez));

        // Verificación: sus tweets dejan de listarse
        em.clear();
        var tweets = TweetRepository.repositorioDe(em).buscarPorIdAutor(idJperez, 0, 10);
        assertEquals(0, tweets.totalElements(), "Los tweets de un usuario borrado no deben listarse");
    }

    @Test
    @DisplayName("Eliminar un usuario elimina sus relaciones de seguimiento")
    void eliminarUsuario_eliminaSusRelacionesDeSeguimiento() {
        // Setup: jperez tiene como seguidor a agomez
        var jperez = repositorio.obtenerPorNombreUsuario(JPEREZ).orElseThrow();
        Integer idJperez = jperez.getId();
        Integer idAgomez = DatosDePrueba.idUsuario(emf, AGOMEZ);

        // Ejercitación: eliminar a jperez
        enTransaccion(() -> repositorio.eliminarUsuario(jperez));

        // Verificación: la relación agomez -> jperez ya no existe
        em.clear();
        boolean sigue = FollowRepository.repositorioDe(em).estaSiguiendo(idAgomez, idJperez);
        assertFalse(sigue, "La relación de seguimiento debe eliminarse junto con la cuenta");
    }

    @Test
    @DisplayName("Eliminar un usuario deja los retweets de otros sobre sus tweets con el origen no disponible")
    void eliminarUsuario_dejaLosRetweetsDeOtrosConElOrigenNoDisponible() {
        // Setup: agomez retuiteó un tweet de jperez
        var jperez = repositorio.obtenerPorNombreUsuario(JPEREZ).orElseThrow();
        Integer idAgomez = DatosDePrueba.idUsuario(emf, AGOMEZ);

        // Ejercitación: eliminar a jperez
        enTransaccion(() -> repositorio.eliminarUsuario(jperez));

        // Verificación: el retweet de agomez sigue existiendo, con el tweet original no disponible
        em.clear();
        var retweets = RetweetRepository.repositorioDe(em).buscarPorIdAutor(idAgomez);
        assertEquals(1, retweets.size(), "El retweet de otra persona debe seguir existiendo");
        assertTrue(retweets.get(0).originalDeleted(), "El tweet original debe figurar como no disponible");
        assertEquals(Tweet.TEXTO_PUBLICACION_NO_DISPONIBLE, retweets.get(0).originalContent(),
                "No debe mostrarse el contenido del tweet original");
    }
}
