package minitwitter.backend.dto;

import java.time.LocalDateTime;

public record OriginalTweetInfo(
        Long id,
        String authorUsername,
        String content,
        LocalDateTime createdAt
) implements TweetInfo {
}
