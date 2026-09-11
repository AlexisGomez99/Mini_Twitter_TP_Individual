package minitwitter.backend.web;

import minitwitter.backend.dto.UserInfo;
import minitwitter.backend.model.DomainException;
import minitwitter.backend.service.TwitterService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {
    private final TwitterService twitterService;

    public UserController(TwitterService twitterService) {
        this.twitterService = twitterService;
    }

    @PostMapping("/users")
    public void addUser(@RequestBody NewUserRequest newUser) {
        this.twitterService.addUser(newUser.username(), newUser.password());
    }

    @GetMapping("/users/{id}")
    public UserInfo getUser(@PathVariable Long id) {
        return this.twitterService.getUserById(id)
                .orElseThrow(() -> new DomainException("Usuario no encontrado: " + id));
    }
    // Caso específico donde sí hace falta buscar por username: por ejemplo, un login,
    // donde todavía no se conoce el id del usuario.
    @GetMapping(value = "/users", params = "username")
    public UserInfo getUserByUsername(@RequestParam String username) {
        return this.twitterService.getUserByUsername(username)
                .orElseThrow(() -> new DomainException("Usuario no encontrado: " + username));
    }

    @GetMapping("/users")
    public List<UserInfo> listUsers() {
        return this.twitterService.listUsers();
    }

    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id) {
        this.twitterService.deleteUser(id);
    }
}
