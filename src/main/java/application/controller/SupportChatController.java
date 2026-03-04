package application.controller;

import application.model.SupportChatRequestModel;
import application.model.SupportChatResponseModel;
import application.service.SupportAssistantService;
import services.SessionContext;

import java.time.LocalDateTime;

public class SupportChatController {

    public static final String CHAT_ENDPOINT_PATH = "/api/support/chat";
    private static final int MAX_MESSAGE_LENGTH = 600;

    private final SupportAssistantService supportAssistantService;

    public SupportChatController(SupportAssistantService supportAssistantService) {
        this.supportAssistantService = supportAssistantService;
    }

    public SupportChatResponseModel postSupportChat(SupportChatRequestModel request) {
        SessionContext.requireNormalUser();

        String userMessage = sanitizeMessage(request == null ? null : request.message());
        if (userMessage.isBlank()) {
            return new SupportChatResponseModel(
                    "Please type a question so I can help with your reservations or hotel search.",
                    LocalDateTime.now()
            );
        }

        String assistantResponse = supportAssistantService.generateSupportReply(userMessage);
        return new SupportChatResponseModel(assistantResponse, LocalDateTime.now());
    }

    private String sanitizeMessage(String rawMessage) {
        if (rawMessage == null) {
            return "";
        }
        String compact = rawMessage
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ")
                .replace('\u0000', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (compact.length() <= MAX_MESSAGE_LENGTH) {
            return compact;
        }
        return compact.substring(0, MAX_MESSAGE_LENGTH).trim();
    }
}
