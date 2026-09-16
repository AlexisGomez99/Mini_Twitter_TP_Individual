package minitwitter.backend.web.dto;

public record NewTweetRequest(Integer userId, String content) {
}
