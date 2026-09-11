package minitwitter.backend.web;

import minitwitter.backend.dto.OriginalTweetInfo;
import minitwitter.backend.dto.TweetInfo;
import minitwitter.backend.service.TwitterService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TweetController {
    private final TwitterService twitterService;

    public TweetController(TwitterService twitterService) {
        this.twitterService = twitterService;
    }

    @PostMapping("/tweets")
    public void createTweet(@RequestBody NewTweetRequest newTweet) {
        this.twitterService.createTweet(newTweet.userId(), newTweet.content());
    }

    @GetMapping("/tweets")
    public List<OriginalTweetInfo> tweetsForUserId(@RequestParam Long userId) {
        return this.twitterService.listTweetsForUserId(userId);
    }

    @GetMapping("/tweets/timeline")
    public List<TweetInfo> timelineForUserId(@RequestParam Long userId) {
        return this.twitterService.getTimelineForUserId(userId);
    }

    @DeleteMapping("/tweets/{tweetId}")
    public void deleteTweet(@PathVariable Long tweetId) {
        this.twitterService.deleteTweet(tweetId);
    }
}
