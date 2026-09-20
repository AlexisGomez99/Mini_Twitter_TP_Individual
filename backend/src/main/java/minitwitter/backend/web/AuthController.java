package minitwitter.backend.web;

import minitwitter.backend.service.TwitterService;
import minitwitter.backend.web.dto.CredentialsUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/registrar")
    public void registrarUsuario(@RequestBody CredentialsUser credentialsUser) {
        this.twitterService.registrarUsuario(credentialsUser.username(), credentialsUser.password());
    }
}
