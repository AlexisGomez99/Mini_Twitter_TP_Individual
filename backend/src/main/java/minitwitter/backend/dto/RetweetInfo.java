package minitwitter.backend.dto;

import java.time.LocalDateTime;

public record RetweetInfo(
        Integer id,
        String authorUsername,
        Integer originalTweetId,
        String originalAuthorUsername,
        String originalContent,
        LocalDateTime createdAt
) implements TweetInfo {
}
