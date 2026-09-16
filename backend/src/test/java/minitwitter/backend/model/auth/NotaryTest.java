package minitwitter.backend.model.auth;

import org.junit.jupiter.api.Test;

import static minitwitter.backend.model.auth.Notary.notary;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotaryTest {

    @Test
    void shouldGenerateATokenAndVerifyItBackToTheSameUserId() {
        // Set up
        int userId = 42;

        // Desarrollo
        String token = notary().generateTokenFor(userId);
        int verifiedUserId = notary().verifyToken(token);

        // Evaluación
        assertThat(verifiedUserId).isEqualTo(userId);
    }

    @Test
    void shouldFailToVerifyAnInvalidToken() {
        // Set up
        String invalidToken = "esto-no-es-un-jwt-valido";

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> notary().verifyToken(invalidToken))
                .isInstanceOf(Exception.class);
    }
}
