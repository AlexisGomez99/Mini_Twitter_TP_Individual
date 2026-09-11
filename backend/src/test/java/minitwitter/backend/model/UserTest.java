package minitwitter.backend.model;

import minitwitter.backend.dto.UserInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void shouldCreateAValidUserSuccessfully() {
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
    void shouldFailWhenUsernameHasLessThanFiveCharacters() {
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
    void shouldFailWhenUsernameHasMoreThanTwentyFiveCharacters() {
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
    void shouldHaveAnEmptyTweetListWhenUserIsCreated() {
        // Set up
        String username = "agomez";
        String password = "password123";

        // Desarrollo
        User user = new User(username, password);

        // Evaluación
        assertThat(user.getTweets()).isNotNull();
        assertThat(user.getTweets()).isEmpty();
    }

    @Test
    void shouldNotBeDeletedWhenCreated() {
        // Set up
        User user = new User("agomez", "password123");

        // Desarrollo / Evaluación

        assertThat(user.isDeleted()).isFalse();
    }

    @Test
    void shouldMarkAUserAsDeleted() {
        // Set up
        User user = new User("agomez", "password123");

        // Desarrollo
        user.markAsDeleted();

        // Evaluación
        assertThat(user.isDeleted()).isTrue();
    }

    @Test
    void shouldConvertAUserToItsInfoWithoutExposingThePassword() {
        // Set up
        User user = new User("agomez", "password123");

        // Desarrollo
        UserInfo info = user.toInfo();

        // Evaluación
        assertThat(info.id()).isEqualTo(user.getId());
        assertThat(info.username()).isEqualTo("agomez");
        assertThat(info.createdAt()).isEqualTo(user.getCreatedAt());
    }
}
