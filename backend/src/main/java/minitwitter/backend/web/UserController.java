package minitwitter.backend.web;

import minitwitter.backend.dto.UserInfo;
import minitwitter.backend.model.DomainException;
import minitwitter.backend.service.TwitterService;
import minitwitter.backend.web.dto.NewUserRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class UserController {
    public static final String ERROR_ELIMINAR_CUENTA_AJENA = "No podés eliminar la cuenta de otra persona";

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
                .orElseThrow(() -> new DomainException(TwitterService.ERROR_USUARIO_NO_ENCONTRADO + id));
    }

    @GetMapping(value = "/users", params = "username")
    public UserInfo obtenerUsuarioPorNombreUsuario(@RequestParam("username") String nombreUsuario,
                                                   @CookieValue("token") String token) {
        this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        return this.servicioTwitter.obtenerUsuarioPorNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new DomainException(TwitterService.ERROR_USUARIO_NO_ENCONTRADO + nombreUsuario));
    }

    @GetMapping("/users")
    public List<UserInfo> listarUsuarios(@CookieValue("token") String token) {
        this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        return this.servicioTwitter.listarUsuarios();
    }

    @DeleteMapping("/users/{id}")
    public void eliminarUsuario(@PathVariable("id") Integer id, @CookieValue("token") String token) {
        int idUsuario = this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        if (!id.equals(idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ERROR_ELIMINAR_CUENTA_AJENA);
        }
        this.servicioTwitter.eliminarUsuario(id);
    }
}
