package minitwitter.backend.model;

import minitwitter.backend.dto.RetweetInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetweetTest {

    private static final String CONTENIDO = "Tweet original";

    private User quienRetuitea;
    private User autorOriginal;
    private OriginalTweet tweetOriginal;

    @BeforeEach
    void antesDeCadaTest() {
        quienRetuitea = new User("agomez", "password123");
        autorOriginal = new User("jperez", "password456");
        tweetOriginal = new OriginalTweet(autorOriginal, CONTENIDO);
    }

    @Test
    @DisplayName("Crear retweet con datos válidos guarda autor y tweet de origen")
    void crearRetweet_guardaAutorYOrigenCuandoSonValidos() {
        // Setup: un usuario y el tweet de otra persona
        // (se crean en el antesDeCadaTest)

        // Ejercitación: crear el retweet
        var retweet = new Retweet(quienRetuitea, tweetOriginal);

        // Verificación: el retweet apunta al tweet original y guarda a quien lo retuitea
        assertSame(quienRetuitea, retweet.getAuthor(), "El autor del retweet debe ser quien retuitea");
        assertSame(tweetOriginal, retweet.getOrigin(), "El origen debe ser el tweet retuiteado");
        assertFalse(retweet.isDeleted(), "Un retweet recién creado no debe estar borrado");
    }

    @Test
    @DisplayName("Crear retweet lanza excepción cuando el tweet de origen es null")
    void crearRetweet_lanzaExcepcionCuandoOrigenEsNull() {
        // Setup: origen nulo
        // (no se necesita setup adicional)

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> new Retweet(quienRetuitea, null));
        assertEquals(Retweet.ERROR_ORIGEN_NULO, excepcion.getMessage(),
                "Debe informar que el tweet de origen no puede ser nulo");
    }

    @Test
    @DisplayName("Crear retweet lanza excepción cuando el autor es null")
    void crearRetweet_lanzaExcepcionCuandoAutorEsNull() {
        // Setup: autor nulo
        // (no se necesita setup adicional)

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> new Retweet(null, tweetOriginal));
        assertEquals(Tweet.ERROR_AUTOR_OBLIGATORIO, excepcion.getMessage(),
                "Debe informar que el tweet necesita un autor");
    }

    @Test
    @DisplayName("Crear retweet lanza excepción cuando se retuitea el propio tweet")
    void crearRetweet_lanzaExcepcionCuandoSeRetuiteaElPropioTweet() {
        // Setup: un tweet escrito por el mismo usuario que lo va a retuitear
        var tweetPropio = new OriginalTweet(quienRetuitea, CONTENIDO);

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> new Retweet(quienRetuitea, tweetPropio));
        assertEquals(Retweet.ERROR_RETWEET_PROPIO, excepcion.getMessage(),
                "Debe informar que no se puede retuitear el propio tweet");
    }

    @Test
    @DisplayName("Convertir a info un retweet disponible muestra los datos del tweet original")
    void convertirAInfo_muestraLosDatosDelOrigenCuandoEstaDisponible() {
        // Setup: un retweet sobre un tweet activo de un autor activo
        var retweet = new Retweet(quienRetuitea, tweetOriginal);

        // Ejercitación: convertirlo a su versión para mostrar
        RetweetInfo info = retweet.toInfo();

        // Verificación: la info muestra quién retuitea y el contenido original
        assertEquals(quienRetuitea.getUsername(), info.authorUsername(), "Debe mostrar quién retuiteó");
        assertEquals(autorOriginal.getUsername(), info.originalAuthorUsername(), "Debe mostrar al autor original");
        assertEquals(CONTENIDO, info.originalContent(), "Debe mostrar el contenido original");
        assertFalse(info.originalDeleted(), "El tweet original disponible no debe figurar como borrado");
    }

    @Test
    @DisplayName("Convertir a info un retweet cuyo origen fue borrado lo marca como no disponible")
    void convertirAInfo_marcaElOrigenComoNoDisponibleCuandoElTweetFueBorrado() {
        // Setup: un retweet cuyo tweet original fue borrado después
        var retweet = new Retweet(quienRetuitea, tweetOriginal);
        tweetOriginal.markAsDeleted();

        // Ejercitación: convertirlo a su versión para mostrar
        RetweetInfo info = retweet.toInfo();

        // Verificación: el retweet sigue existiendo pero sin el contenido original
        assertTrue(info.originalDeleted(), "Debe indicar que el tweet original está borrado");
        assertEquals(Tweet.TEXTO_PUBLICACION_NO_DISPONIBLE, info.originalContent(),
                "No debe mostrar el contenido de un tweet borrado");
    }

    @Test
    @DisplayName("Convertir a info un retweet de una cuenta eliminada muestra el nombre de cuenta eliminada")
    void convertirAInfo_muestraCuentaEliminadaCuandoQuienRetuiteaFueBorrado() {
        // Setup: un retweet cuyo autor borró su cuenta después
        var retweet = new Retweet(quienRetuitea, tweetOriginal);
        quienRetuitea.markAsDeleted();

        // Ejercitación: convertirlo a su versión para mostrar
        RetweetInfo info = retweet.toInfo();

        // Verificación: no se muestra el nombre de quien retuiteó
        assertEquals(Tweet.NOMBRE_CUENTA_ELIMINADA, info.authorUsername(),
                "Una cuenta eliminada no debe mostrar su nombre de usuario");
    }

    @Test
    @DisplayName("Convertir a info un retweet cuyo autor original borró la cuenta muestra cuenta eliminada")
    void convertirAInfo_muestraCuentaEliminadaCuandoElAutorOriginalFueBorrado() {
        // Setup: un retweet sobre el tweet de alguien que borró su cuenta
        var retweet = new Retweet(quienRetuitea, tweetOriginal);
        autorOriginal.markAsDeleted();

        // Ejercitación: convertirlo a su versión para mostrar
        RetweetInfo info = retweet.toInfo();

        // Verificación: no se muestra el nombre del autor original
        assertEquals(Tweet.NOMBRE_CUENTA_ELIMINADA, info.originalAuthorUsername(),
                "El autor original con cuenta eliminada no debe mostrar su nombre");
    }

    @Test
    @DisplayName("Convertir a info un retweet cuyo origen es otro retweet usa los textos de reemplazo")
    void convertirAInfo_usaTextosDeReemplazoCuandoElOrigenEsOtroRetweet() {
        // Setup: un retweet de un retweet (el origen no es un tweet original)
        var retweetDelAutorOriginal = new Retweet(autorOriginal, new OriginalTweet(quienRetuitea, CONTENIDO));
        var tercero = new User("mlopez", "password789");
        var retweet = new Retweet(tercero, retweetDelAutorOriginal);

        // Ejercitación: convertirlo a su versión para mostrar
        RetweetInfo info = retweet.toInfo();

        // Verificación: no hay contenido propio que mostrar del origen
        assertEquals(Tweet.NOMBRE_CUENTA_ELIMINADA, info.originalAuthorUsername(),
                "Un origen que no es un tweet original no tiene autor para mostrar");
        assertEquals(Tweet.TEXTO_PUBLICACION_NO_DISPONIBLE, info.originalContent(),
                "Un origen que no es un tweet original no tiene contenido para mostrar");
    }
}
