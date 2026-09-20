package minitwitter.backend.web;

import minitwitter.backend.dto.OriginalTweetInfo;
import minitwitter.backend.dto.TweetInfo;
import minitwitter.backend.service.TwitterService;
import minitwitter.backend.web.dto.NewTweetRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TweetController {
    private final TwitterService twitterService;

    public TweetController(TwitterService twitterService) {
        this.twitterService = twitterService;
    }

    @PostMapping("/tweets")
    public void createTweet(@RequestBody NewTweetRequest newTweet, @CookieValue("token") String token) {
        this.twitterService.verificarTokenAndGetIdUsuario(token);
        this.twitterService.createTweet(newTweet.userId(), newTweet.content());
    }

    @GetMapping("/tweets")
    public List<OriginalTweetInfo> tweetsForUserId(@RequestParam Integer userId, @CookieValue("token") String token) {
        this.twitterService.verificarTokenAndGetIdUsuario(token);
        return this.twitterService.listTweetsForUserId(userId);
    }

    @GetMapping("/tweets/timeline")
    public List<TweetInfo> timelineForUserId(@RequestParam Integer userId, @CookieValue("token") String token) {
        this.twitterService.verificarTokenAndGetIdUsuario(token);
        return this.twitterService.getTimelineForUserId(userId);
    }

    @DeleteMapping("/tweets/{tweetId}")
    public void deleteTweet(@PathVariable Integer tweetId, @CookieValue("token") String token) {
        this.twitterService.verificarTokenAndGetIdUsuario(token);
        this.twitterService.deleteTweet(tweetId);
    }
}
