package minitwitter.backend.web.dto;

public record FollowRequest(Integer followerId, Integer followedId) {
}
