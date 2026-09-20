package minitwitter.backend.dto;

import java.time.LocalDateTime;

public record UserInfo(
        Integer id,
        String username,
        LocalDateTime createdAt
) {
}
