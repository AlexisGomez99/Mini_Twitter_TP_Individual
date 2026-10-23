package minitwitter.backend.web;

import minitwitter.backend.dto.UserInfo;
import minitwitter.backend.service.TwitterService;
import minitwitter.backend.web.dto.FollowRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class FollowController {
    private final TwitterService servicioTwitter;

    public FollowController(TwitterService servicioTwitter) {
        this.servicioTwitter = servicioTwitter;
    }

    @PostMapping("/follows")
    public void seguir(@RequestBody FollowRequest pedidoSeguir, @CookieValue("token") String token) {
        this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        this.servicioTwitter.seguir(pedidoSeguir.followerId(), pedidoSeguir.followedId());
    }

    @DeleteMapping("/follows")
    public void dejarDeSeguir(@RequestParam("followerId") Integer idSeguidor, @RequestParam("followedId") Integer idSeguido, @CookieValue("token") String token) {
        this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        this.servicioTwitter.dejarDeSeguir(idSeguidor, idSeguido);
    }

    @GetMapping("/follows/is-following")
    public boolean estaSiguiendo(@RequestParam("followerId") Integer idSeguidor, @RequestParam("followedId") Integer idSeguido, @CookieValue("token") String token) {
        this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        return this.servicioTwitter.estaSiguiendo(idSeguidor, idSeguido);
    }

    @GetMapping("/follows/followers")
    public List<UserInfo> seguidores(@RequestParam("userId") Integer idUsuario, @CookieValue("token") String token) {
        this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        return this.servicioTwitter.obtenerSeguidores(idUsuario);
    }

    @GetMapping("/follows/following")
    public List<UserInfo> seguidos(@RequestParam("userId") Integer idUsuario, @CookieValue("token") String token) {
        this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        return this.servicioTwitter.obtenerSeguidos(idUsuario);
    }
}
