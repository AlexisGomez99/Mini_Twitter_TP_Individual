package minitwitter.backend.dto;

import java.time.LocalDateTime;

public record UserInfo(
        Long id,
        String username,
        LocalDateTime createdAt
) {
}
