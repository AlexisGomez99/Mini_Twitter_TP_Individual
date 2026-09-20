package minitwitter.backend.web.dto;

public record NewRetweetRequest(Integer userId, Integer originTweetId) {
}
