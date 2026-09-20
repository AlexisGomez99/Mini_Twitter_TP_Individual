package minitwitter.backend.web;

import minitwitter.backend.dto.RetweetInfo;
import minitwitter.backend.service.TwitterService;
import minitwitter.backend.web.dto.NewRetweetRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RetweetController {
    private final TwitterService twitterService;

    public RetweetController(TwitterService twitterService) {
        this.twitterService = twitterService;
    }

    @PostMapping("/retweets")
    public void createRetweet(@RequestBody NewRetweetRequest newRetweet, @CookieValue("token") String token) {
        this.twitterService.verificarTokenAndGetIdUsuario(token);
        this.twitterService.createRetweet(newRetweet.userId(), newRetweet.originTweetId());
    }

    @GetMapping("/retweets")
    public List<RetweetInfo> retweetsForUserId(@RequestParam Integer userId, @CookieValue("token") String token) {
        this.twitterService.verificarTokenAndGetIdUsuario(token);
        return this.twitterService.listRetweetsForUserId(userId);
    }

    @DeleteMapping("/retweets/{retweetId}")
    public void deleteRetweet(@PathVariable Integer retweetId, @CookieValue("token") String token) {
        this.twitterService.verificarTokenAndGetIdUsuario(token);
        this.twitterService.deleteRetweet(retweetId);
    }
}
