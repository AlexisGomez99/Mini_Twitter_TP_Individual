package minitwitter.backend.web;

public record NewTweetRequest(Long userId, String content) {
}
