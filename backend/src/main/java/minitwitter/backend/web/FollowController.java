package minitwitter.backend.web;

import minitwitter.backend.dto.UserInfo;
import minitwitter.backend.service.TwitterService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class FollowController {
    private final TwitterService twitterService;

    public FollowController(TwitterService twitterService) {
        this.twitterService = twitterService;
    }

    @PostMapping("/follows")
    public void follow(@RequestBody FollowRequest followRequest) {
        this.twitterService.follow(followRequest.followerId(), followRequest.followedId());
    }

    @DeleteMapping("/follows")
    public void unfollow(@RequestParam Long followerId, @RequestParam Long followedId) {
        this.twitterService.unfollow(followerId, followedId);
    }

    @GetMapping("/follows/is-following")
    public boolean isFollowing(@RequestParam Long followerId, @RequestParam Long followedId) {
        return this.twitterService.isFollowing(followerId, followedId);
    }

    @GetMapping("/follows/followers")
    public List<UserInfo> followers(@RequestParam Long userId) {
        return this.twitterService.getFollowers(userId);
    }

    @GetMapping("/follows/following")
    public List<UserInfo> following(@RequestParam Long userId) {
        return this.twitterService.getFollowing(userId);
    }
}
