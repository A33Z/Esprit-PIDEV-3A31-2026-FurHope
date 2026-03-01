package application.model;

import java.time.LocalDateTime;

public record SupportChatResponseModel(
        String response,
        LocalDateTime createdAt
) {
}
