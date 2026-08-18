package minitwitter.backend.model;

import minitwitter.backend.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FollowTest {

    @Test
    void deberiaCrearUnaRelacionDeSeguimientoValidaYRegistrarLaFecha() {
        // Set up
        User follower = new User("agomez", "password123");
        User followed = new User("jperez", "password456");

        // Desarrollo
        Follow follow = new Follow(follower, followed);

        // Evaluación
        assertThat(follow.getFollower()).isEqualTo(follower);
        assertThat(follow.getFollowed()).isEqualTo(followed);
        assertThat(follow.getFollowedAt()).isNotNull();
    }

    @Test
    void deberiaFallarSiUnUsuarioIntentaSeguirseASiMismo() {
        // Set up
        User user = new User("agomez", "password123");

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new Follow(user, user))
                .isInstanceOf(DomainException.class)
                .hasMessage("Un usuario no puede seguirse a sí mismo.");
    }

    @Test
    void deberiaFallarSiElFollowerEsNulo() {
        // Set up
        User followed = new User("jperez", "password456");

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new Follow(null, followed))
                .isInstanceOf(DomainException.class)
                .hasMessage("El seguidor y el seguido son obligatorios.");
    }

    @Test
    void deberiaFallarSiElFollowedEsNulo() {
        // Set up
        User follower = new User("agomez", "password123");

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new Follow(follower, null))
                .isInstanceOf(DomainException.class)
                .hasMessage("El seguidor y el seguido son obligatorios.");
    }
}
