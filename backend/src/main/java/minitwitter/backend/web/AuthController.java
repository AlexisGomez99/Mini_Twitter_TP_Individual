package minitwitter.backend.web;

import minitwitter.backend.dto.UserInfo;
import minitwitter.backend.service.TwitterService;
import minitwitter.backend.web.dto.CredentialsUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthController {
    public static final String NOMBRE_COOKIE = "token";
    public static final int DURACION_COOKIE_SEGUNDOS = 180; // 3 mins
    public static final String ERROR_SESION_INVALIDA = "Sesión inválida";

    private final TwitterService servicioTwitter;

    public AuthController(TwitterService servicioTwitter) {
        this.servicioTwitter = servicioTwitter;
    }

    @PostMapping("/login")
    public ResponseEntity<Void> iniciarSesion(@RequestBody CredentialsUser credenciales) {
        var token = this.servicioTwitter.iniciarSesion(credenciales.username(), credenciales.password());
        //agregar token a la response como cookie:
        var cookie = ResponseCookie.from(NOMBRE_COOKIE, token)
                .httpOnly(true)
                .path("/")
                .secure(false) // must change for PROD
                .sameSite("Strict")
                .maxAge(DURACION_COOKIE_SEGUNDOS)
                .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> cerrarSesion() {
        var cookie = ResponseCookie.from(NOMBRE_COOKIE, "")
                .httpOnly(true)
                .path("/")
                .secure(false) // must change for PROD
                .sameSite("Strict")
                .maxAge(0)
                .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @PostMapping("/registrar")
    public void registrarUsuario(@RequestBody CredentialsUser credenciales) {
        this.servicioTwitter.registrarUsuario(credenciales.username(), credenciales.password());
    }

    @GetMapping("/me")
    public UserInfo usuarioLogueado(@CookieValue(NOMBRE_COOKIE) String token) {
        int idUsuario = this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        return this.servicioTwitter.obtenerUsuarioPorId(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ERROR_SESION_INVALIDA));
    }
}
