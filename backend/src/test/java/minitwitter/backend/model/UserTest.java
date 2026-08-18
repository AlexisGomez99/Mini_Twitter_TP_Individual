package minitwitter.backend.model;

import minitwitter.backend.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void deberiaCrearUnUsuarioValidoExitosamente() {
        // Set up
        String username = "agomez";
        String password = "password123";

        // Desarrollo
        User user = new User(username, password);

        // Evaluación
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getPassword()).isEqualTo(password);
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    void deberiaFallarSiElUsernameTieneMenosDeCincoCaracteres() {
        // Set up
        String username = "abcd";
        String password = "password123";

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new User(username, password))
                .isInstanceOf(DomainException.class)
                .hasMessage("El nombre de usuario debe tener entre 5 y 25 caracteres.");
    }

    @Test
    void deberiaFallarSiElUsernameTieneMasDeVeinticincoCaracteres() {
        // Set up
        String username = "a".repeat(26);
        String password = "password123";

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new User(username, password))
                .isInstanceOf(DomainException.class)
                .hasMessage("El nombre de usuario debe tener entre 5 y 25 caracteres.");
    }

    @Test
    void deberiaTenerUnaListaDeTweetsVaciaAlCrearUnUsuario() {
        // Set up
        String username = "agomez";
        String password = "password123";

        // Desarrollo
        User user = new User(username, password);

        // Evaluación
        assertThat(user.getTweets()).isNotNull();
        assertThat(user.getTweets()).isEmpty();
    }
}
