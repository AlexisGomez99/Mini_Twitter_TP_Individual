package minitwitter.backend.dto;

import java.time.LocalDateTime;

public record RetweetInfo(
        Long id,
        String authorUsername,
        Long originalTweetId,
        String originalAuthorUsername,
        String originalContent,
        LocalDateTime createdAt
) implements TweetInfo {
}
