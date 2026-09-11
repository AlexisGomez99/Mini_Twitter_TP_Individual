package minitwitter.backend.dto;

import java.time.LocalDateTime;

public sealed interface TweetInfo permits OriginalTweetInfo, RetweetInfo {
    LocalDateTime createdAt();
}
