package minitwitter.backend.web;

import minitwitter.backend.dto.UserInfo;
import minitwitter.backend.service.TwitterService;
import minitwitter.backend.web.dto.FollowRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class FollowController {
    private final TwitterService twitterService;

    public FollowController(TwitterService twitterService) {
        this.twitterService = twitterService;
    }

    @PostMapping("/follows")
    public void follow(@RequestBody FollowRequest followRequest, @CookieValue("token") String token) {
        this.twitterService.verificarTokenAndGetIdUsuario(token);
        this.twitterService.follow(followRequest.followerId(), followRequest.followedId());
    }

    @DeleteMapping("/follows")
    public void unfollow(@RequestParam Integer followerId, @RequestParam Integer followedId,
                          @CookieValue("token") String token) {
        this.twitterService.verificarTokenAndGetIdUsuario(token);
        this.twitterService.unfollow(followerId, followedId);
    }

    @GetMapping("/follows/is-following")
    public boolean isFollowing(@RequestParam Integer followerId, @RequestParam Integer followedId,
                                @CookieValue("token") String token) {
        this.twitterService.verificarTokenAndGetIdUsuario(token);
        return this.twitterService.isFollowing(followerId, followedId);
    }

    @GetMapping("/follows/followers")
    public List<UserInfo> followers(@RequestParam Integer userId, @CookieValue("token") String token) {
        this.twitterService.verificarTokenAndGetIdUsuario(token);
        return this.twitterService.getFollowers(userId);
    }

    @GetMapping("/follows/following")
    public List<UserInfo> following(@RequestParam Integer userId, @CookieValue("token") String token) {
        this.twitterService.verificarTokenAndGetIdUsuario(token);
        return this.twitterService.getFollowing(userId);
    }
}
