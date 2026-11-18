package minitwitter.backend.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FollowTest {

    private final User seguidor = new User("agomez", "password123");
    private final User seguido = new User("jperez", "password456");

    @Test
    @DisplayName("Crear follow con dos usuarios distintos guarda seguidor, seguido y fecha")
    void crearFollow_guardaSeguidorSeguidoYFecha() {
        // Setup: dos usuarios distintos
        // (los usuarios se crean como campos de la clase)

        // Ejercitación: crear la relación de seguimiento
        var follow = new Follow(seguidor, seguido);

        // Verificación: la relación guarda a ambos usuarios y registra la fecha
        assertSame(seguidor, follow.getFollower(), "El seguidor debe ser el primer usuario recibido");
        assertSame(seguido, follow.getFollowed(), "El seguido debe ser el segundo usuario recibido");
        assertNotNull(follow.getFollowedAt(), "La relación debe registrar la fecha en que se creó");
    }

    @Test
    @DisplayName("Crear follow lanza excepción cuando el seguidor es null")
    void crearFollow_lanzaExcepcionCuandoSeguidorEsNull() {
        // Setup: seguidor nulo
        // (no se necesita setup adicional)

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> new Follow(null, seguido));
        assertEquals(Follow.ERROR_PARTICIPANTES_OBLIGATORIOS, excepcion.getMessage(),
                "Debe informar que seguidor y seguido son obligatorios");
    }

    @Test
    @DisplayName("Crear follow lanza excepción cuando el seguido es null")
    void crearFollow_lanzaExcepcionCuandoSeguidoEsNull() {
        // Setup: seguido nulo
        // (no se necesita setup adicional)

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> new Follow(seguidor, null));
        assertEquals(Follow.ERROR_PARTICIPANTES_OBLIGATORIOS, excepcion.getMessage(),
                "Debe informar que seguidor y seguido son obligatorios");
    }

    @Test
    @DisplayName("Crear follow lanza excepción cuando un usuario intenta seguirse a sí mismo")
    void crearFollow_lanzaExcepcionCuandoUsuarioSeSigueASiMismo() {
        // Setup: un único usuario que será seguidor y seguido a la vez
        // (el usuario se crea como campo de la clase)

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> new Follow(seguidor, seguidor));
        assertEquals(Follow.ERROR_SEGUIRSE_A_SI_MISMO, excepcion.getMessage(),
                "Debe informar que un usuario no puede seguirse a sí mismo");
    }
}
