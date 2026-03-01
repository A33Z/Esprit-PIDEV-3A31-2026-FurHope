package com.esprit.services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

public class AIResponseService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final String DEFAULT_PROVIDER = "AUTO";

    private static final String SYSTEM_PROMPT = """
            You are FurHope Support AI, a professional and intelligent customer support assistant for an animal shelter platform.

            You must carefully analyze each client complaint and adapt to the emotional tone.

            Steps:
            1. Identify the core issue.
            2. Detect emotional tone (angry, frustrated, worried, confused, neutral).
            3. Choose the best response strategy:
               - angry/frustrated: de-escalate, validate feelings, give immediate concrete actions.
               - worried: reassure with clear ownership and timeline.
               - confused: explain simply and ask precise clarification questions.
               - neutral: concise and solution-focused.
            4. Respond directly to the exact issue described.
            5. Provide practical next steps and expected follow-up.
            6. Maintain warm and professional tone.

            Rules:
            - Do NOT give generic responses.
            - Reference the actual problem described.
            - If the complaint lacks information, politely ask for clarification.
            - Never blame the client.
            - Never invent facts or actions that are not supported by the complaint.
            - Do the analysis internally. Output only the final client-ready response.
            - Keep response between 120-200 words.
            - Sound human and natural.
            """;

    private final HttpClient httpClient;
    private final Properties appProperties;
    private final LocalReclamationResponseModel localModel;

    public AIResponseService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.appProperties = loadAppProperties();
        this.localModel = new LocalReclamationResponseModel();
    }

    public String generateSmartResponse(String complaintText) throws AIResponseException {
        if (complaintText == null || complaintText.trim().isEmpty()) {
            throw new AIResponseException("Complaint text is empty.");
        }

        String provider = firstNonBlank(resolve("AI_PROVIDER", "ai.replies.provider"), DEFAULT_PROVIDER).toUpperCase();
        if ("LOCAL".equals(provider)) {
            return localModel.generate(complaintText);
        }

        String apiKey = resolve("OPENAI_API_KEY", "ai.replies.api_key");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return localModel.generate(complaintText);
        }

        String model = firstNonBlank(resolve("OPENAI_MODEL", "ai.replies.model"), DEFAULT_MODEL);
        String body = buildRequestBody(model, complaintText.trim());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_URL))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiKey.trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String responseBody = truncate(response.body(), 300);
                if ("OPENAI".equals(provider)) {
                    throw new AIResponseException("OpenAI API error (" + response.statusCode() + "): " + responseBody);
                }
                return localModel.generate(complaintText);
            }
            String content = extractAssistantContent(response.body());
            if (content == null || content.isBlank()) {
                if ("OPENAI".equals(provider)) {
                    throw new AIResponseException("OpenAI returned an empty response.");
                }
                return localModel.generate(complaintText);
            }
            return content.trim();
        } catch (AIResponseException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if ("OPENAI".equals(provider)) {
                throw new AIResponseException("Failed to generate AI response: " + e.getMessage(), e);
            }
            return localModel.generate(complaintText);
        } catch (Exception e) {
            if ("OPENAI".equals(provider)) {
                throw new AIResponseException("Unexpected AI generation error: " + e.getMessage(), e);
            }
            return localModel.generate(complaintText);
        }
    }

    private String buildRequestBody(String model, String complaintText) throws AIResponseException {
        try {
            JsonNode payload = MAPPER.createObjectNode()
                    .put("model", model)
                    .put("temperature", 0.3)
                    .set("messages", MAPPER.createArrayNode()
                            .add(MAPPER.createObjectNode()
                                    .put("role", "system")
                                    .put("content", SYSTEM_PROMPT))
                            .add(MAPPER.createObjectNode()
                                    .put("role", "user")
                                    .put("content", complaintText)));
            return MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new AIResponseException("Unable to build OpenAI request payload.", e);
        }
    }

    private String extractAssistantContent(String rawBody) throws IOException {
        JsonNode root = MAPPER.readTree(rawBody);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return null;
        }
        JsonNode first = choices.get(0);
        JsonNode content = first.path("message").path("content");
        return content.isMissingNode() || content.isNull() ? null : content.asText();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    private String firstNonBlank(String first, String fallback) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        return fallback;
    }

    private String resolve(String envKey, String propertyKey) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }
        String propValue = appProperties.getProperty(propertyKey);
        if (propValue != null && !propValue.trim().isEmpty()) {
            return propValue.trim();
        }
        return null;
    }

    private Properties loadAppProperties() {
        Properties props = new Properties();
        try (InputStream in = AIResponseService.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception ignored) {
        }
        try {
            File secrets = new File("ai.secrets.properties");
            if (secrets.isFile()) {
                try (FileInputStream in = new FileInputStream(secrets)) {
                    props.load(in);
                }
            }
        } catch (Exception ignored) {
        }
        return props;
    }

    public static final class AIResponseException extends Exception {
        public AIResponseException(String message) {
            super(message);
        }

        public AIResponseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
