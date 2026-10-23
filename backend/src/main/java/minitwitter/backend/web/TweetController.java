package minitwitter.backend.web;

import minitwitter.backend.dto.OriginalTweetInfo;
import minitwitter.backend.dto.PageResponse;
import minitwitter.backend.dto.TweetInfo;
import minitwitter.backend.service.TwitterService;
import minitwitter.backend.web.dto.NewTweetRequest;
import org.springframework.web.bind.annotation.*;

@RestController
public class TweetController {
    private final TwitterService servicioTwitter;

    public TweetController(TwitterService servicioTwitter) {
        this.servicioTwitter = servicioTwitter;
    }

    @PostMapping("/tweets")
    public void crearTweet(@RequestBody NewTweetRequest nuevoTweet, @CookieValue("token") String token) {
        this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        this.servicioTwitter.crearTweet(nuevoTweet.userId(), nuevoTweet.content());
    }

    @GetMapping("/tweets")
    public PageResponse<OriginalTweetInfo> tweetsDelUsuario(@RequestParam("userId") Integer idUsuario, @RequestParam(name = "page", defaultValue = "0") int pagina, @CookieValue("token") String token) {
        this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        return this.servicioTwitter.listarTweetsDelUsuario(idUsuario, pagina);
    }

    // El usuario sale del token: es el feed de quien está logueado.
    @GetMapping("/tweets/feed")
    public PageResponse<TweetInfo> feed(@RequestParam(name = "page", defaultValue = "0") int pagina, @CookieValue("token") String token) {
        int idUsuario = this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        return this.servicioTwitter.obtenerFeedDelUsuario(idUsuario, pagina);
    }

    @DeleteMapping("/tweets/{tweetId}")
    public void eliminarTweet(@PathVariable("tweetId") Integer idTweet, @CookieValue("token") String token) {
        int idUsuario = this.servicioTwitter.verificarTokenYObtenerIdUsuario(token);
        this.servicioTwitter.eliminarTweet(idUsuario, idTweet);
    }
}
