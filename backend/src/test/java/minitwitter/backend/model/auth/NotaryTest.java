package minitwitter.backend.model.auth;

import org.junit.jupiter.api.Test;

import static minitwitter.backend.model.auth.Notary.notary;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotaryTest {

    @Test
    void deberiaGenerarUnTokenYVerificarloDevolviendoElMismoIdDeUsuario() {
        // Set up
        int idUsuario = 42;

        // Desarrollo
        String token = notary().generateTokenFor(idUsuario);
        int idUsuarioVerificado = notary().verifyToken(token);

        // Evaluación
        assertThat(idUsuarioVerificado).isEqualTo(idUsuario);
    }

    @Test
    void deberiaFallarAlVerificarUnTokenInvalido() {
        // Set up
        String tokenInvalido = "esto-no-es-un-jwt-valido";

        // Desarrollo / Evaluación
        assertThatThrownBy(() -> notary().verifyToken(tokenInvalido))
                .isInstanceOf(Exception.class);
    }
}
