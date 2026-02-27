package services;

import com.google.gson.*;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AnthropicChatService {

    private static final String API_KEY = "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";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions"; // ✅ Groq
    private static final String MODEL = "llama-3.3-70b-versatile"; // ✅ Modèle Groq gratuit

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final Gson gson = new Gson();

    private static final String SYSTEM_PROMPT =
            "Tu es un assistant médical et vétérinaire sympathique. " +
                    "Si quelqu'un te salue (bonjour, salut, hello...), réponds chaleureusement. " +
                    "Tu peux répondre sur les vaccins humains et animaux, " +
                    "les symptômes de maladies, les âges recommandés pour les examens, " +
                    "les soins préventifs pour animaux de compagnie. " +
                    "Réponds toujours en français, de manière claire et concise. " +
                    "Rappelle toujours de consulter un médecin ou vétérinaire pour un diagnostic officiel.";

    public String sendMessage(String userMessage) throws IOException {

        // ✅ Format Groq (différent de Gemini)
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", SYSTEM_PROMPT);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);

        JsonArray messages = new JsonArray();
        messages.add(systemMsg);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.add("messages", messages);
        body.addProperty("max_tokens", 1024);
        body.addProperty("temperature", 0.7);

        RequestBody requestBody = RequestBody.create(
                gson.toJson(body),
                MediaType.get("application/json")
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY) // ✅ Groq utilise Bearer
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body().string();
                throw new IOException("Erreur Groq " + response.code() + ": " + errorBody);
            }
            String responseBody = response.body().string();
            System.out.println("Réponse Groq : " + responseBody);

            // ✅ Format réponse Groq (différent de Gemini)
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            return json.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        }
    }
}