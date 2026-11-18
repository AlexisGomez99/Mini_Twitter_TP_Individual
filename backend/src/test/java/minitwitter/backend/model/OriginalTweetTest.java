package minitwitter.backend.model;

import minitwitter.backend.dto.OriginalTweetInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OriginalTweetTest {

    private static final String CONTENIDO = "Mi primer tweet";

    private User autor;

    @BeforeEach
    void antesDeCadaTest() {
        autor = new User("agomez", "password123");
    }

    @Test
    @DisplayName("Crear tweet con datos válidos guarda autor, contenido y fecha de creación")
    void crearTweet_guardaLosDatosCuandoSonValidos() {
        // Setup: un autor y un contenido válidos
        // (el autor se crea en el antesDeCadaTest)

        // Ejercitación: crear el tweet
        var tweet = new OriginalTweet(autor, CONTENIDO);

        // Verificación: el tweet guarda lo recibido y nace sin estar borrado
        assertSame(autor, tweet.getAuthor(), "El autor debe ser el recibido");
        assertEquals(CONTENIDO, tweet.getContent(), "El contenido debe ser el recibido");
        assertNotNull(tweet.getCreatedAt(), "El tweet debe registrar la fecha de creación");
        assertFalse(tweet.isDeleted(), "Un tweet recién creado no debe estar borrado");
    }

    @Test
    @DisplayName("Crear tweet lanza excepción cuando el contenido es null")
    void crearTweet_lanzaExcepcionCuandoContenidoEsNull() {
        // Setup: contenido nulo
        // (no se necesita setup adicional)

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> new OriginalTweet(autor, null));
        assertEquals(OriginalTweet.ERROR_CONTENIDO_INVALIDO, excepcion.getMessage(),
                "Debe informar que el contenido no es válido");
    }

    @Test
    @DisplayName("Crear tweet lanza excepción cuando el contenido está vacío")
    void crearTweet_lanzaExcepcionCuandoContenidoEstaVacio() {
        // Setup: contenido vacío
        String contenidoVacio = "";

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> new OriginalTweet(autor, contenidoVacio));
        assertEquals(OriginalTweet.ERROR_CONTENIDO_INVALIDO, excepcion.getMessage(),
                "Debe informar que el contenido no es válido");
    }

    @Test
    @DisplayName("Crear tweet lanza excepción cuando el contenido solo tiene espacios")
    void crearTweet_lanzaExcepcionCuandoContenidoSoloTieneEspacios() {
        // Setup: contenido formado solo por espacios
        String contenidoEnBlanco = "     ";

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> new OriginalTweet(autor, contenidoEnBlanco));
        assertEquals(OriginalTweet.ERROR_CONTENIDO_INVALIDO, excepcion.getMessage(),
                "Debe informar que el contenido no es válido");
    }

    @Test
    @DisplayName("Crear tweet acepta un contenido de un solo caracter")
    void crearTweet_aceptaContenidoDeUnSoloCaracter() {
        // Setup: contenido con el largo mínimo posible
        String contenidoMinimo = "a";

        // Ejercitación: crear el tweet
        var tweet = new OriginalTweet(autor, contenidoMinimo);

        // Verificación: el contenido se acepta tal cual
        assertEquals(contenidoMinimo, tweet.getContent(), "Un contenido de un caracter debe ser válido");
    }

    @Test
    @DisplayName("Crear tweet acepta un contenido con exactamente el largo máximo")
    void crearTweet_aceptaContenidoConElLargoMaximo() {
        // Setup: contenido con exactamente el largo máximo
        String contenidoMaximo = "a".repeat(OriginalTweet.LARGO_MAXIMO_CONTENIDO);

        // Ejercitación: crear el tweet
        var tweet = new OriginalTweet(autor, contenidoMaximo);

        // Verificación: el contenido se acepta tal cual
        assertEquals(contenidoMaximo, tweet.getContent(), "El largo máximo exacto debe ser válido");
    }

    @Test
    @DisplayName("Crear tweet lanza excepción cuando el contenido supera el máximo")
    void crearTweet_lanzaExcepcionCuandoContenidoSuperaElMaximo() {
        // Setup: contenido un caracter más largo que el máximo
        String contenidoLargo = "a".repeat(OriginalTweet.LARGO_MAXIMO_CONTENIDO + 1);

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> new OriginalTweet(autor, contenidoLargo));
        assertEquals(OriginalTweet.ERROR_CONTENIDO_INVALIDO, excepcion.getMessage(),
                "Debe informar que el contenido no es válido");
    }

    @Test
    @DisplayName("Crear tweet lanza excepción cuando el autor es null")
    void crearTweet_lanzaExcepcionCuandoAutorEsNull() {
        // Setup: autor nulo
        // (no se necesita setup adicional)

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> new OriginalTweet(null, CONTENIDO));
        assertEquals(Tweet.ERROR_AUTOR_OBLIGATORIO, excepcion.getMessage(),
                "Debe informar que el tweet necesita un autor");
    }

    @Test
    @DisplayName("Marcar un tweet como borrado conserva su contenido guardado")
    void marcarComoBorrado_conservaElContenido() {
        // Setup: un tweet activo
        var tweet = new OriginalTweet(autor, CONTENIDO);

        // Ejercitación: marcarlo como borrado
        tweet.markAsDeleted();

        // Verificación: queda borrado pero el texto sigue guardado
        assertTrue(tweet.isDeleted(), "El tweet debe quedar marcado como borrado");
        assertEquals(CONTENIDO, tweet.getContent(), "El contenido debe seguir guardado aunque el tweet esté borrado");
    }

    @Test
    @DisplayName("Convertir a info un tweet disponible muestra autor y contenido")
    void convertirAInfo_muestraAutorYContenidoCuandoEstaDisponible() {
        // Setup: un tweet activo de un autor activo
        var tweet = new OriginalTweet(autor, CONTENIDO);

        // Ejercitación: convertirlo a su versión para mostrar
        OriginalTweetInfo info = tweet.toInfo();

        // Verificación: la info muestra los datos reales
        assertEquals(autor.getUsername(), info.authorUsername(), "Debe mostrar el nombre real del autor");
        assertEquals(CONTENIDO, info.content(), "Debe mostrar el contenido real");
        assertEquals(tweet.getCreatedAt(), info.createdAt(), "Debe mostrar la fecha de creación");
    }

    @Test
    @DisplayName("Convertir a info un tweet borrado muestra el texto de publicación no disponible")
    void convertirAInfo_muestraTextoDeReemplazoCuandoElTweetEstaBorrado() {
        // Setup: un tweet borrado
        var tweet = new OriginalTweet(autor, CONTENIDO);
        tweet.markAsDeleted();

        // Ejercitación: convertirlo a su versión para mostrar
        OriginalTweetInfo info = tweet.toInfo();

        // Verificación: en lugar del contenido se muestra el aviso
        assertEquals(Tweet.TEXTO_PUBLICACION_NO_DISPONIBLE, info.content(),
                "Un tweet borrado no debe mostrar su contenido");
    }

    @Test
    @DisplayName("Convertir a info un tweet de una cuenta eliminada muestra el nombre de cuenta eliminada")
    void convertirAInfo_muestraCuentaEliminadaCuandoElAutorEstaBorrado() {
        // Setup: un tweet cuyo autor fue borrado después de publicarlo
        var tweet = new OriginalTweet(autor, CONTENIDO);
        autor.markAsDeleted();

        // Ejercitación: convertirlo a su versión para mostrar
        OriginalTweetInfo info = tweet.toInfo();

        // Verificación: no se muestra el nombre del autor
        assertEquals(Tweet.NOMBRE_CUENTA_ELIMINADA, info.authorUsername(),
                "Una cuenta eliminada no debe mostrar su nombre de usuario");
    }
}
