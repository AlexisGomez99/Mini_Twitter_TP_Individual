package minitwitter.backend.model;

import minitwitter.backend.dto.UserInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void deberiaCrearUnUsuarioValido() {
        // Set up
        String nombreUsuario = "agomez";
        String contrasenia = "password123";

        // Desarrollo
        User usuario = new User(nombreUsuario, contrasenia);

        // Evaluación
        assertThat(usuario.getUsername()).isEqualTo(nombreUsuario);
        assertThat(usuario.getPassword()).isEqualTo(contrasenia);
        assertThat(usuario.getCreatedAt()).isNotNull();
    }

    @Test
    void deberiaFallarCuandoFaltaLaContraseniaOEstaEnBlanco() {
        // Set up
        String nombreUsuario = "agomez";

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new User(nombreUsuario, null))
                .isInstanceOf(DomainException.class)
                .hasMessage("La contraseña es obligatoria.");
        assertThatThrownBy(() -> new User(nombreUsuario, "   "))
                .isInstanceOf(DomainException.class)
                .hasMessage("La contraseña es obligatoria.");
    }

    @Test
    void deberiaFallarCuandoElNombreDeUsuarioTieneMenosDeCincoCaracteres() {
        // Set up
        String nombreUsuario = "abcd";
        String contrasenia = "password123";

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new User(nombreUsuario, contrasenia))
                .isInstanceOf(DomainException.class)
                .hasMessage("El nombre de usuario debe tener entre 5 y 25 caracteres.");
    }

    @Test
    void deberiaFallarCuandoElNombreDeUsuarioTieneMasDeVeinticincoCaracteres() {
        // Set up
        String nombreUsuario = "a".repeat(26);
        String contrasenia = "password123";

        // Desarrollo
        // (la excepción se lanza dentro del constructor, por eso se evalúa con el lambda)

        // Evaluación
        assertThatThrownBy(() -> new User(nombreUsuario, contrasenia))
                .isInstanceOf(DomainException.class)
                .hasMessage("El nombre de usuario debe tener entre 5 y 25 caracteres.");
    }

    @Test
    void deberiaConvertirUnUsuarioEnSuInfoSinExponerLaContrasenia() {
        // Set up
        User usuario = new User("agomez", "password123");

        // Desarrollo
        UserInfo info = usuario.toInfo();

        // Evaluación
        assertThat(info.id()).isEqualTo(usuario.getId());
        assertThat(info.username()).isEqualTo("agomez");
        assertThat(info.createdAt()).isEqualTo(usuario.getCreatedAt());
    }
}
