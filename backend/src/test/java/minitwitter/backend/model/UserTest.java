package minitwitter.backend.model;

import minitwitter.backend.dto.UserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    private static final String NOMBRE_VALIDO = "agomez";
    private static final String CONTRASENIA_VALIDA = "password123";

    @Test
    @DisplayName("Crear usuario con datos válidos guarda nombre, contraseña y fecha de creación")
    void crearUsuario_guardaLosDatosCuandoSonValidos() {
        // Setup: un nombre y una contraseña válidos
        // (no se necesita setup adicional)

        // Ejercitación: crear el usuario
        var usuario = new User(NOMBRE_VALIDO, CONTRASENIA_VALIDA);

        // Verificación: el usuario guarda lo recibido y nace sin estar borrado
        assertEquals(NOMBRE_VALIDO, usuario.getUsername(), "El nombre de usuario guardado debe ser el recibido");
        assertEquals(CONTRASENIA_VALIDA, usuario.getPassword(), "La contraseña guardada debe ser la recibida");
        assertNotNull(usuario.getCreatedAt(), "El usuario debe registrar la fecha de creación");
        assertFalse(usuario.isDeleted(), "Un usuario recién creado no debe estar borrado");
    }

    @Test
    @DisplayName("Crear usuario lanza excepción cuando el nombre es null")
    void crearUsuario_lanzaExcepcionCuandoNombreEsNull() {
        // Setup: nombre nulo
        // (no se necesita setup adicional)

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> new User(null, CONTRASENIA_VALIDA));
        assertEquals(User.ERROR_NOMBRE_USUARIO_INVALIDO, excepcion.getMessage(),
                "Debe informar que el nombre de usuario no es válido");
    }

    @Test
    @DisplayName("Crear usuario lanza excepción cuando el nombre está vacío")
    void crearUsuario_lanzaExcepcionCuandoNombreEstaVacio() {
        // Setup: nombre vacío
        String nombreVacio = "";

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> new User(nombreVacio, CONTRASENIA_VALIDA));
        assertEquals(User.ERROR_NOMBRE_USUARIO_INVALIDO, excepcion.getMessage(),
                "Debe informar que el nombre de usuario no es válido");
    }

    @Test
    @DisplayName("Crear usuario lanza excepción cuando el nombre tiene un caracter menos que el mínimo")
    void crearUsuario_lanzaExcepcionCuandoNombreNoLlegaAlMinimo() {
        // Setup: nombre un caracter más corto que el largo mínimo permitido
        String nombreCorto = "a".repeat(User.LARGO_MINIMO_NOMBRE_USUARIO - 1);

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> new User(nombreCorto, CONTRASENIA_VALIDA));
        assertEquals(User.ERROR_NOMBRE_USUARIO_INVALIDO, excepcion.getMessage(),
                "Debe informar que el nombre de usuario no es válido");
    }

    @Test
    @DisplayName("Crear usuario acepta un nombre con exactamente el largo mínimo")
    void crearUsuario_aceptaNombreConElLargoMinimo() {
        // Setup: nombre con exactamente el largo mínimo
        String nombreMinimo = "a".repeat(User.LARGO_MINIMO_NOMBRE_USUARIO);

        // Ejercitación: crear el usuario
        var usuario = new User(nombreMinimo, CONTRASENIA_VALIDA);

        // Verificación: el nombre se acepta tal cual
        assertEquals(nombreMinimo, usuario.getUsername(), "El largo mínimo exacto debe ser válido");
    }

    @Test
    @DisplayName("Crear usuario acepta un nombre con exactamente el largo máximo")
    void crearUsuario_aceptaNombreConElLargoMaximo() {
        // Setup: nombre con exactamente el largo máximo
        String nombreMaximo = "a".repeat(User.LARGO_MAXIMO_NOMBRE_USUARIO);

        // Ejercitación: crear el usuario
        var usuario = new User(nombreMaximo, CONTRASENIA_VALIDA);

        // Verificación: el nombre se acepta tal cual
        assertEquals(nombreMaximo, usuario.getUsername(), "El largo máximo exacto debe ser válido");
    }

    @Test
    @DisplayName("Crear usuario lanza excepción cuando el nombre supera el máximo")
    void crearUsuario_lanzaExcepcionCuandoNombreSuperaElMaximo() {
        // Setup: nombre un caracter más largo que el máximo permitido
        String nombreLargo = "a".repeat(User.LARGO_MAXIMO_NOMBRE_USUARIO + 1);

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> new User(nombreLargo, CONTRASENIA_VALIDA));
        assertEquals(User.ERROR_NOMBRE_USUARIO_INVALIDO, excepcion.getMessage(),
                "Debe informar que el nombre de usuario no es válido");
    }

    @Test
    @DisplayName("Crear usuario lanza excepción cuando la contraseña es null")
    void crearUsuario_lanzaExcepcionCuandoContraseniaEsNull() {
        // Setup: contraseña nula
        // (no se necesita setup adicional)

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> new User(NOMBRE_VALIDO, null));
        assertEquals(User.ERROR_CONTRASENIA_OBLIGATORIA, excepcion.getMessage(),
                "Debe informar que la contraseña es obligatoria");
    }

    @Test
    @DisplayName("Crear usuario lanza excepción cuando la contraseña está vacía")
    void crearUsuario_lanzaExcepcionCuandoContraseniaEstaVacia() {
        // Setup: contraseña vacía
        String contraseniaVacia = "";

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> new User(NOMBRE_VALIDO, contraseniaVacia));
        assertEquals(User.ERROR_CONTRASENIA_OBLIGATORIA, excepcion.getMessage(),
                "Debe informar que la contraseña es obligatoria");
    }

    @Test
    @DisplayName("Crear usuario lanza excepción cuando la contraseña solo tiene espacios")
    void crearUsuario_lanzaExcepcionCuandoContraseniaSoloTieneEspacios() {
        // Setup: contraseña formada solo por espacios
        String contraseniaEnBlanco = "   ";

        // Ejercitación y Verificación
        var excepcion = assertThrows(DomainException.class,
                () -> new User(NOMBRE_VALIDO, contraseniaEnBlanco));
        assertEquals(User.ERROR_CONTRASENIA_OBLIGATORIA, excepcion.getMessage(),
                "Debe informar que la contraseña es obligatoria");
    }

    @Test
    @DisplayName("Marcar un usuario como borrado deja su estado en borrado")
    void marcarComoBorrado_dejaElUsuarioBorrado() {
        // Setup: un usuario activo
        var usuario = new User(NOMBRE_VALIDO, CONTRASENIA_VALIDA);

        // Ejercitación: marcarlo como borrado
        usuario.markAsDeleted();

        // Verificación: queda borrado
        assertTrue(usuario.isDeleted(), "El usuario debe quedar marcado como borrado");
    }

    @Test
    @DisplayName("Convertir un usuario a info copia nombre y fecha de creación")
    void convertirAInfo_copiaNombreYFechaDeCreacion() {
        // Setup: un usuario válido
        var usuario = new User(NOMBRE_VALIDO, CONTRASENIA_VALIDA);

        // Ejercitación: convertirlo a su versión para mostrar
        UserInfo info = usuario.toInfo();

        // Verificación: la info refleja los datos públicos del usuario
        assertEquals(NOMBRE_VALIDO, info.username(), "La info debe tener el nombre del usuario");
        assertEquals(usuario.getCreatedAt(), info.createdAt(), "La info debe tener la fecha de creación del usuario");
    }
}
