package minitwitter.backend.web;

public record NewRetweetRequest(Long userId, Long originTweetId) {
}
