package minitwitter.backend.model.auth;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static minitwitter.backend.model.auth.Notary.notary;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotaryTest {

    private static final int ID_USUARIO = 7;

    @Test
    @DisplayName("Generar un token y verificarlo devuelve el mismo id de usuario")
    void generarToken_permiteRecuperarElIdDelUsuario() {
        // Setup: un token generado para un usuario
        String token = notary().generateTokenFor(ID_USUARIO);

        // Ejercitación: verificar el token
        int idRecuperado = notary().verifyToken(token);

        // Verificación: el id que sale es el que se usó para generarlo
        assertEquals(ID_USUARIO, idRecuperado, "El token debe contener el id del usuario");
    }

    @Test
    @DisplayName("Verificar un texto que no es un token lanza excepción")
    void verificarToken_lanzaExcepcionCuandoElTextoNoEsUnToken() {
        // Setup: un texto cualquiera que no tiene formato de token
        String textoInvalido = "esto-no-es-un-token";

        // Ejercitación y Verificación
        assertThrows(JwtException.class, () -> notary().verifyToken(textoInvalido),
                "Un texto sin formato de token debe ser rechazado");
    }

    @Test
    @DisplayName("Verificar un token con la firma alterada lanza excepción")
    void verificarToken_lanzaExcepcionCuandoLaFirmaFueAlterada() {
        // Setup: un token válido al que se le cambia el último caracter de la firma
        String token = notary().generateTokenFor(ID_USUARIO);
        char ultimo = token.charAt(token.length() - 1);
        String tokenAlterado = token.substring(0, token.length() - 1) + (ultimo == 'A' ? 'B' : 'A');

        // Ejercitación y Verificación
        assertThrows(JwtException.class, () -> notary().verifyToken(tokenAlterado),
                "Un token con la firma alterada debe ser rechazado");
    }

    @Test
    @DisplayName("Verificar un token vacío lanza excepción")
    void verificarToken_lanzaExcepcionCuandoElTokenEstaVacio() {
        // Setup: token vacío
        String tokenVacio = "";

        // Ejercitación y Verificación
        assertThrows(IllegalArgumentException.class, () -> notary().verifyToken(tokenVacio),
                "Un token vacío debe ser rechazado");
    }

    @Test
    @DisplayName("Verificar un token null lanza excepción")
    void verificarToken_lanzaExcepcionCuandoElTokenEsNull() {
        // Setup: token nulo
        // (no se necesita setup adicional)

        // Ejercitación y Verificación
        assertThrows(IllegalArgumentException.class, () -> notary().verifyToken(null),
                "Un token nulo debe ser rechazado");
    }
}
