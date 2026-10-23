package minitwitter.backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import minitwitter.backend.model.DomainException;
import minitwitter.backend.model.OriginalTweet;
import minitwitter.backend.model.User;
import minitwitter.backend.util.EmfBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TwitterServiceTest {

    private static EntityManagerFactory emf;

    private EntityManager em;
    private TwitterService servicioTwitter;

    private User agomez;
    private User jperez;
    private OriginalTweet tweetJperez;
    private Integer idAgomez;
    private Integer idJperez;

    @BeforeAll
    static void antesDeTodo() {
        emf = new EmfBuilder().h2EnMemoria().conRecreacionDeEsquema().construir();
    }

    @BeforeEach
    void antesDeCadaTest() {
        // Truncate: limpia todos los datos antes de cada test, para que no queden restos del anterior.
        emf.getSchemaManager().truncate();

        em = emf.createEntityManager();
        servicioTwitter = new TwitterService(emf);

        agomez = new User("agomez", "password123");
        jperez = new User("jperez", "password456");
        tweetJperez = new OriginalTweet(jperez, "Tweet de Luis");

        em.getTransaction().begin();
        em.persist(agomez);
        em.persist(jperez);
        em.persist(tweetJperez);
        em.getTransaction().commit();

        idAgomez = agomez.getId();
        idJperez = jperez.getId();
    }

    // --- Auth ---

    @Test
    void deberiaIniciarSesionConCredencialesValidasYDevolverUnTokenDelUsuario() {
        // Set up (agomez cargado en beforeEach con password "password123")

        // Desarrollo
        String token = servicioTwitter.iniciarSesion("agomez", "password123");

        // Evaluación: el token generado corresponde al id real de agomez
        assertThat(servicioTwitter.verificarTokenYObtenerIdUsuario(token)).isEqualTo(idAgomez);
    }

    @Test
    void deberiaFallarAlIniciarSesionConUnaContraseniaIncorrecta() {
        // Set up (agomez cargado en beforeEach)

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> servicioTwitter.iniciarSesion("agomez", "password_incorrecta"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void deberiaFallarAlIniciarSesionConUnNombreDeUsuarioInexistente() {
        // Set up (usuarios cargados en beforeEach, ninguno se llama "no_existe")

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> servicioTwitter.iniciarSesion("no_existe", "cualquier_clave"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void deberiaFallarAlVerificarUnTokenInvalido() {
        // Set up
        String tokenInvalido = "esto-no-es-un-jwt-valido";

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> servicioTwitter.verificarTokenYObtenerIdUsuario(tokenInvalido))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void deberiaRegistrarUnUsuarioNuevoYDevolverSuId() {
        // Set up (agomez y jperez ya existen; se registra uno distinto)

        // Desarrollo
        Integer idUsuarioNuevo = servicioTwitter.registrarUsuario("nuevo_user", "clave12345");

        // Evaluación
        assertThat(idUsuarioNuevo).isEqualTo(servicioTwitter.obtenerUsuarioPorNombreUsuario("nuevo_user").orElseThrow().id());
    }

    @Test
    void deberiaFallarAlRegistrarUnNombreDeUsuarioQueYaExiste() {
        // Set up (agomez cargado en beforeEach)

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> servicioTwitter.registrarUsuario("agomez", "otra_clave"))
                .isInstanceOf(RuntimeException.class);
    }

    // --- Usuarios ---

    @Test
    void deberiaNoHacerNadaAlEliminarUnUsuarioInexistente() {
        // Set up (usuarios cargados en beforeEach, ninguno tiene id 999999)

        // Desarrollo / Evaluación: no debería lanzar ninguna excepción
        assertThatCode(() -> servicioTwitter.eliminarUsuario(999999)).doesNotThrowAnyException();
    }

    // --- Tweets ---

    @Test
    void deberiaCrearUnTweetParaUnUsuarioExistente() {
        // Set up (usuarios cargados en beforeEach)

        // Desarrollo
        servicioTwitter.crearTweet(idAgomez, "Mi primer tweet");

        // Evaluación
        var tweets = servicioTwitter.listarTweetsDelUsuario(idAgomez, 0).content();
        assertThat(tweets).hasSize(1);
        assertThat(tweets.get(0).content()).isEqualTo("Mi primer tweet");
    }

    @Test
    void deberiaFallarAlCrearUnTweetParaUnUsuarioInexistente() {
        // Set up (usuarios cargados en beforeEach, ninguno tiene id 999999)

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> servicioTwitter.crearTweet(999999, "contenido"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void deberiaFallarAlCrearUnTweetParaUnUsuarioBorrado() {
        // Set up
        servicioTwitter.eliminarUsuario(idAgomez);

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> servicioTwitter.crearTweet(idAgomez, "contenido"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void deberiaEliminarUnTweetExistente() {
        // Set up
        servicioTwitter.crearTweet(idAgomez, "Tweet a borrar");
        Integer idTweet = servicioTwitter.listarTweetsDelUsuario(idAgomez, 0).content().get(0).id();

        // Desarrollo
        servicioTwitter.eliminarTweet(idAgomez, idTweet);

        // Evaluación
        assertThat(servicioTwitter.listarTweetsDelUsuario(idAgomez, 0).content()).isEmpty();
    }

    @Test
    void deberiaFallarAlEliminarUnTweetDeOtroUsuario() {
        // Set up (el tweet de jperez se cargó en beforeEach)

        // Desarrollo / Evaluación: agomez no puede borrar el tweet de jperez
        assertThatThrownBy(() -> servicioTwitter.eliminarTweet(idAgomez, tweetJperez.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No podés eliminar un tweet de otra persona");
        assertThat(servicioTwitter.listarTweetsDelUsuario(idJperez, 0).content()).hasSize(1);
    }

    @Test
    void deberiaNoHacerNadaAlEliminarUnTweetInexistente() {
        // Set up (base sin ningún tweet con id 999999)

        // Desarrollo / Evaluación: no debería lanzar ninguna excepción
        assertThatCode(() -> servicioTwitter.eliminarTweet(idAgomez, 999999)).doesNotThrowAnyException();
    }

    @Test
    void deberiaDevolverElFeedEnPaginasDeVeinte() {
        // Set up: agomez publica 21 tweets
        for (int i = 1; i <= 21; i++) {
            servicioTwitter.crearTweet(idAgomez, "Tweet " + i);
        }

        // Desarrollo
        var primeraPagina = servicioTwitter.obtenerFeedDelUsuario(idAgomez, 0);
        var segundaPagina = servicioTwitter.obtenerFeedDelUsuario(idAgomez, 1);

        // Evaluación: 20 en la primera (con más páginas) y el que sobra en la segunda
        assertThat(primeraPagina.content()).hasSize(20);
        assertThat(primeraPagina.hasNext()).isTrue();
        assertThat(primeraPagina.totalElements()).isEqualTo(21);
        assertThat(primeraPagina.totalPages()).isEqualTo(2);
        assertThat(segundaPagina.content()).hasSize(1);
        assertThat(segundaPagina.hasNext()).isFalse();
    }

    @Test
    void deberiaDevolverLosTweetsDelUsuarioEnPaginasDeDiez() {
        // Set up: agomez publica 11 tweets
        for (int i = 1; i <= 11; i++) {
            servicioTwitter.crearTweet(idAgomez, "Tweet " + i);
        }

        // Desarrollo
        var primeraPagina = servicioTwitter.listarTweetsDelUsuario(idAgomez, 0);
        var segundaPagina = servicioTwitter.listarTweetsDelUsuario(idAgomez, 1);

        // Evaluación
        assertThat(primeraPagina.content()).hasSize(10);
        assertThat(primeraPagina.hasNext()).isTrue();
        assertThat(segundaPagina.content()).hasSize(1);
        assertThat(segundaPagina.hasNext()).isFalse();
    }

    @Test
    void deberiaFallarAlPedirUnaPaginaNegativa() {
        // Set up (usuarios cargados en beforeEach)

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> servicioTwitter.obtenerFeedDelUsuario(idAgomez, -1))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> servicioTwitter.listarTweetsDelUsuario(idAgomez, -1))
                .isInstanceOf(DomainException.class);
    }

    // --- Retweets ---

    @Test
    void deberiaCrearUnRetweetParaUnUsuarioYUnTweetExistentes() {
        // Set up (usuarios y tweet cargados en beforeEach)

        // Desarrollo
        servicioTwitter.crearRetweet(idAgomez, tweetJperez.getId());

        // Evaluación
        var retweets = servicioTwitter.listarRetweetsDelUsuario(idAgomez);
        assertThat(retweets).hasSize(1);
        assertThat(retweets.get(0).originalAuthorUsername()).isEqualTo("jperez");
    }

    @Test
    void deberiaFallarAlCrearUnRetweetParaUnUsuarioInexistente() {
        // Set up (jperezTweet cargado en beforeEach, ningún usuario tiene id 999999)

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> servicioTwitter.crearRetweet(999999, tweetJperez.getId()))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void deberiaFallarAlCrearUnRetweetDeUnTweetInexistente() {
        // Set up (agomez cargado en beforeEach, ningún tweet tiene id 999999)

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> servicioTwitter.crearRetweet(idAgomez, 999999))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void deberiaFallarAlCrearUnRetweetParaUnUsuarioBorrado() {
        // Set up
        servicioTwitter.eliminarUsuario(idAgomez);

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> servicioTwitter.crearRetweet(idAgomez, tweetJperez.getId()))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void deberiaFallarAlCrearUnRetweetDeUnTweetBorrado() {
        // Set up
        servicioTwitter.eliminarTweet(idJperez, tweetJperez.getId());

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> servicioTwitter.crearRetweet(idAgomez, tweetJperez.getId()))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void deberiaEliminarUnRetweetExistente() {
        // Set up
        servicioTwitter.crearRetweet(idAgomez, tweetJperez.getId());
        Integer idRetweet = servicioTwitter.listarRetweetsDelUsuario(idAgomez).get(0).id();

        // Desarrollo
        servicioTwitter.eliminarRetweet(idRetweet);

        // Evaluación
        assertThat(servicioTwitter.listarRetweetsDelUsuario(idAgomez)).isEmpty();
    }

    @Test
    void deberiaNoHacerNadaAlEliminarUnRetweetInexistente() {
        // Set up (base sin ningún retweet con id 999999)

        // Desarrollo / Evaluación: no debería lanzar ninguna excepción
        assertThatCode(() -> servicioTwitter.eliminarRetweet(999999)).doesNotThrowAnyException();
    }

    // --- Follows ---

    @Test
    void deberiaCrearUnaRelacionDeSeguimiento() {
        // Set up (usuarios cargados en beforeEach)

        // Desarrollo
        servicioTwitter.seguir(idAgomez, idJperez);

        // Evaluación
        assertThat(servicioTwitter.estaSiguiendo(idAgomez, idJperez)).isTrue();
    }

    @Test
    void deberiaNoHacerNadaAlSeguirAAlguienQueYaSeSigue() {
        // Set up
        servicioTwitter.seguir(idAgomez, idJperez);

        // Desarrollo / Evaluación: seguir de nuevo no lanza excepción ni duplica la relación
        assertThatCode(() -> servicioTwitter.seguir(idAgomez, idJperez)).doesNotThrowAnyException();
        assertThat(servicioTwitter.obtenerSeguidores(idJperez)).hasSize(1);
    }

    @Test
    void deberiaFallarAlSeguirCuandoElSeguidorNoExiste() {
        // Set up (usuarios cargados en beforeEach, ninguno tiene id 999999)

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> servicioTwitter.seguir(999999, idJperez))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void deberiaFallarAlSeguirCuandoElSeguidoNoExiste() {
        // Set up (usuarios cargados en beforeEach, ninguno tiene id 999999)

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> servicioTwitter.seguir(idAgomez, 999999))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void deberiaFallarAlSeguirCuandoElSeguidorEstaBorrado() {
        // Set up
        servicioTwitter.eliminarUsuario(idAgomez);

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> servicioTwitter.seguir(idAgomez, idJperez))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void deberiaFallarAlSeguirCuandoElSeguidoEstaBorrado() {
        // Set up
        servicioTwitter.eliminarUsuario(idJperez);

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> servicioTwitter.seguir(idAgomez, idJperez))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void deberiaEliminarLaRelacionDeSeguimientoAlDejarDeSeguir() {
        // Set up
        servicioTwitter.seguir(idAgomez, idJperez);

        // Desarrollo
        servicioTwitter.dejarDeSeguir(idAgomez, idJperez);

        // Evaluación
        assertThat(servicioTwitter.estaSiguiendo(idAgomez, idJperez)).isFalse();
    }

    @Test
    void deberiaFallarAlDejarDeSeguirCuandoElSeguidorNoExiste() {
        // Set up (usuarios cargados en beforeEach, ninguno tiene id 999999)

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> servicioTwitter.dejarDeSeguir(999999, idJperez))
                .isInstanceOf(DomainException.class);
    }

}
