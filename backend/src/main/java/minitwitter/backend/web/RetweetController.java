package minitwitter.backend.web;

import minitwitter.backend.dto.RetweetInfo;
import minitwitter.backend.service.TwitterService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RetweetController {
    private final TwitterService twitterService;

    public RetweetController(TwitterService twitterService) {
        this.twitterService = twitterService;
    }

    @PostMapping("/retweets")
    public void createRetweet(@RequestBody NewRetweetRequest newRetweet) {
        this.twitterService.createRetweet(newRetweet.userId(), newRetweet.originTweetId());
    }

    @GetMapping("/retweets")
    public List<RetweetInfo> retweetsForUserId(@RequestParam Long userId) {
        return this.twitterService.listRetweetsForUserId(userId);
    }

    @DeleteMapping("/retweets/{retweetId}")
    public void deleteRetweet(@PathVariable Long retweetId) {
        this.twitterService.deleteRetweet(retweetId);
    }
}
