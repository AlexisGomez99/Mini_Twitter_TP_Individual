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

        import java.util.List;

@RestController
public class AuthController {
    private final TwitterService twitterService;

    public AuthController(TwitterService twitterService) {
        this.twitterService = twitterService;
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody CredentialsUser credentialsUser) {
        var token = this.twitterService.login(credentialsUser.username(), credentialsUser.password());
        //agregar token a la response como cookie:
        var cookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .path("/")
                .secure(false) // must change for PROD
                .sameSite("Strict")
                .maxAge(180) // 1 hora alineado al jwt deberia estar
                .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    // La cookie es HttpOnly: el frontend no puede borrarla, así que la expira el backend.
    // Tiene que repetir name/path/sameSite/secure del login para que el navegador la
    // reconozca como la misma. No exige estar logueado: cerrar sesión con la cookie ya
    // vencida o ausente tiene que funcionar igual.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        var cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .path("/")
                .secure(false) // must change for PROD
                .sameSite("Strict")
                .maxAge(0)
                .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @PostMapping("/registrar")
    public void registrarUsuario(@RequestBody CredentialsUser credentialsUser) {
        this.twitterService.registrarUsuario(credentialsUser.username(), credentialsUser.password());
    }

    // Quién está logueado: el frontend no puede leer la cookie (HttpOnly), así que
    // consulta acá al cargar la página. Si el usuario del token ya fue borrado, la
    // sesión deja de ser válida.
    @GetMapping("/me")
    public UserInfo me(@CookieValue("token") String token) {
        int userId = this.twitterService.verificarTokenAndGetIdUsuario(token);
        return this.twitterService.getUserById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida"));
    }
}
