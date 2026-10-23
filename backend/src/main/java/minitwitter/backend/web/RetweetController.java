package minitwitter.backend.web;

import minitwitter.backend.dto.RetweetInfo;
import minitwitter.backend.service.TwitterService;
import minitwitter.backend.web.dto.NewRetweetRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RetweetController {
    private final TwitterService servicioTwitter;

    public RetweetController(TwitterService servicioTwitter) {
        this.servicioTwitter = servicioTwitter;
    }

    @PostMapping("/retweets")
    public void crearRetweet(@RequestBody NewRetweetRequest nuevoRetweet, @CookieValue("token") String token) {
        this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        this.servicioTwitter.crearRetweet(nuevoRetweet.userId(), nuevoRetweet.originTweetId());
    }

    @GetMapping("/retweets")
    public List<RetweetInfo> retweetsDelUsuario(@RequestParam("userId") Integer idUsuario, @CookieValue("token") String token) {
        this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        return this.servicioTwitter.listarRetweetsDelUsuario(idUsuario);
    }

    @DeleteMapping("/retweets/{retweetId}")
    public void eliminarRetweet(@PathVariable("retweetId") Integer idRetweet, @CookieValue("token") String token) {
        this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        this.servicioTwitter.eliminarRetweet(idRetweet);
    }
}
