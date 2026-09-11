package minitwitter.backend.web;

public record FollowRequest(Long followerId, Long followedId) {
}
