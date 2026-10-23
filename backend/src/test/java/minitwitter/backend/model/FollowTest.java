package minitwitter.backend.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FollowTest {

    @Test
    void deberiaCrearUnaRelacionDeSeguimientoValidaYRegistrarLaFecha() {
        // Set up
        User seguidor = new User("agomez", "password123");
        User seguido = new User("jperez", "password456");

        // Desarrollo
        Follow seguir = new Follow(seguidor, seguido);

        // Evaluación
        assertThat(seguir.getFollower()).isEqualTo(seguidor);
        assertThat(seguir.getFollowed()).isEqualTo(seguido);
        assertThat(seguir.getFollowedAt()).isNotNull();
    }

    @Test
    void deberiaFallarCuandoUnUsuarioIntentaSeguirseASiMismo() {
        // Set up
        User usuario = new User("agomez", "password123");

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new Follow(usuario, usuario))
                .isInstanceOf(DomainException.class)
                .hasMessage("Un usuario no puede seguirse a sí mismo.");
    }

    @Test
    void deberiaFallarCuandoElSeguidorEsNulo() {
        // Set up
        User seguido = new User("jperez", "password456");

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new Follow(null, seguido))
                .isInstanceOf(DomainException.class)
                .hasMessage("El seguidor y el seguido son obligatorios.");
    }

    @Test
    void deberiaFallarCuandoElSeguidoEsNulo() {
        // Set up
        User seguidor = new User("agomez", "password123");

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new Follow(seguidor, null))
                .isInstanceOf(DomainException.class)
                .hasMessage("El seguidor y el seguido son obligatorios.");
    }
}
