package services;

import utils.Config;
import okhttp3.*;
import java.io.IOException;

public class SmsService {

    private static final String INFOBIP_KEY = Config.get("INFOBIP_KEY");
    private static final String BASE_URL = Config.get("INFOBIP_BASE_URL");

    public static void sendSms(String toPhone, String message) {
        try {
            OkHttpClient client = new OkHttpClient();

            String json = "{\n" +
                    "  \"messages\": [\n" +
                    "    {\n" +
                    "      \"destinations\": [{\"to\": \"" + toPhone + "\"}],\n" +
                    "      \"from\": \"FurHope\",\n" +
                    "      \"text\": \"" + message + "\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";

            RequestBody body = RequestBody.create(
                    json, MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "/sms/2/text/advanced")
                    .post(body)
                    .addHeader("Authorization", "App " + INFOBIP_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build();

            Response response = client.newCall(request).execute();
            System.out.println("SMS envoyé : " + response.code() + " - " + response.body().string());

        } catch (IOException e) {
            System.out.println("❌ Erreur SMS : " + e.getMessage());
        }
    }
}