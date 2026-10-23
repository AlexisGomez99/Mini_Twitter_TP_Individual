package minitwitter.backend.web;

import minitwitter.backend.dto.UserInfo;
import minitwitter.backend.model.DomainException;
import minitwitter.backend.service.TwitterService;
import minitwitter.backend.web.dto.NewUserRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {
    private final TwitterService servicioTwitter;

    public UserController(TwitterService servicioTwitter) {
        this.servicioTwitter = servicioTwitter;
    }

    @PostMapping("/users")
    public void agregarUsuario(@RequestBody NewUserRequest nuevoUsuario) {
        this.servicioTwitter.agregarUsuario(nuevoUsuario.username(), nuevoUsuario.password());
    }

    @GetMapping("/users/{id}")
    public UserInfo obtenerUsuario(@PathVariable("id") Integer id, @CookieValue("token") String token) {
        this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        return this.servicioTwitter.obtenerUsuarioPorId(id)
                .orElseThrow(() -> new DomainException("Usuario no encontrado: " + id));
    }

    // Caso específico donde sí hace falta buscar por username: por ejemplo, un login,
    // donde todavía no se conoce el id del usuario.
    @GetMapping(value = "/users", params = "username")
    public UserInfo obtenerUsuarioPorNombreUsuario(@RequestParam("username") String nombreUsuario,
                                                   @CookieValue("token") String token) {
        this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        return this.servicioTwitter.obtenerUsuarioPorNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new DomainException("Usuario no encontrado: " + nombreUsuario));
    }

    @GetMapping("/users")
    public List<UserInfo> listarUsuarios(@CookieValue("token") String token) {
        this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        return this.servicioTwitter.listarUsuarios();
    }

    @DeleteMapping("/users/{id}")
    public void eliminarUsuario(@PathVariable("id") Integer id, @CookieValue("token") String token) {
        this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        this.servicioTwitter.eliminarUsuario(id);
    }
}
