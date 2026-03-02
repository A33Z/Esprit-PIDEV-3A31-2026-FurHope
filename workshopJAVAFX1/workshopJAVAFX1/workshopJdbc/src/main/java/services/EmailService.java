package services;

import com.google.gson.*;
import okhttp3.*;
import java.io.IOException;
import utils.Config;

public class EmailService {

    private static final String API_KEY = Config.get("BREVO_KEY");
    private static final String API_URL = Config.get("BREVO_URL");
    private static final String SENDER_EMAIL = Config.get("SENDER_EMAIL");
    private static final String SENDER_NAME  = Config.get("SENDER_NAME");

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();

    public void sendEmail(String toEmail, String toName, String subject, String content) throws IOException {

        JsonObject sender = new JsonObject();
        sender.addProperty("email", SENDER_EMAIL);
        sender.addProperty("name", SENDER_NAME);

        JsonObject recipient = new JsonObject();
        recipient.addProperty("email", toEmail);
        recipient.addProperty("name", toName);

        JsonArray recipients = new JsonArray();
        recipients.add(recipient);

        JsonObject body = new JsonObject();
        body.add("sender", sender);
        body.add("to", recipients);
        body.addProperty("subject", subject);
        body.addProperty("htmlContent", content);

        RequestBody requestBody = RequestBody.create(
                gson.toJson(body),
                MediaType.get("application/json")
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("api-key", API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Erreur Brevo " + response.code() + ": " + response.body().string());
            }
            System.out.println("✅ Email envoyé à " + toEmail);
        }
    }

    public void notifyVetNewRdv(String vetEmail, String vetNom, String clientNum, String description) {
        try {
            String[] info     = parseDescription(description);
            String animalNom  = info[0].isEmpty() ? "Non renseigné" : info[0];
            String animalType = info[1].isEmpty() ? ""              : " le " + info[1];
            String date       = info[2].isEmpty() ? "Non renseignée": info[2];
            String heure      = info[3].isEmpty() ? ""              : " à " + info[3];
            String motif      = info[4].isEmpty() ? "Non renseigné" : info[4];

            String subject = "🔔 Nouveau Rendez-vous - FurHope";
            String content =
                    "<div style='margin:0;padding:0;background-color:#f9fafb;font-family:Arial,sans-serif;'>" +


                            "<div style='background:linear-gradient(135deg,#f97316,#ea580c);padding:40px 30px;text-align:center;'>" +
                            "<h1 style='color:white;margin:0;font-size:28px;'>🐾 FurHope</h1>" +
                            "<p style='color:#ffedd5;margin:8px 0 0;font-size:15px;'>Clinique Vétérinaire</p>" +
                            "</div>" +


                            "<div style='background:white;max-width:600px;margin:0 auto;padding:40px 30px;'>" +
                            "<h2 style='color:#f97316;font-size:22px;margin-top:0;'>📋 Nouveau Rendez-vous</h2>" +
                            "<p style='color:#374151;font-size:15px;'>Bonjour Dr. <b>" + vetNom + "</b>,</p>" +
                            "<p style='color:#374151;font-size:15px;'>Votre prochain client est <b>" +
                            animalNom + "</b>" + animalType +
                            ". Le rendez-vous est prévu le <b>" + date + heure + "</b>.</p>" +


                            "<div style='background:#fff7ed;border-left:4px solid #f97316;border-radius:8px;padding:20px;margin:24px 0;'>" +
                            "<table style='width:100%;border-collapse:collapse;'>" +
                            "<tr>" +
                            "<td style='padding:10px;color:#92400e;font-weight:bold;width:40%;'>📱 Téléphone</td>" +
                            "<td style='padding:10px;color:#1f2937;'>" + clientNum + "</td>" +
                            "</tr>" +
                            "<tr style='background:white;'>" +
                            "<td style='padding:10px;color:#92400e;font-weight:bold;'>📝 Motif</td>" +
                            "<td style='padding:10px;color:#1f2937;'>" + motif + "</td>" +
                            "</tr>" +
                            "</table>" +
                            "</div>" +

                            "<div style='text-align:center;margin:30px 0;'>" +
                            "<p style='color:#6b7280;font-size:14px;'>Connectez-vous à votre espace pour accepter ou refuser ce rendez-vous.</p>" +
                            "</div>" +

                            "<hr style='border:none;border-top:1px solid #f3f4f6;margin:30px 0;'/>" +
                            "<p style='color:#9ca3af;font-size:12px;text-align:center;'>🐾 FurHope — Clinique Vétérinaire<br/>Cet email est automatique, merci de ne pas y répondre.</p>" +
                            "</div></div>";

            sendEmail(vetEmail, "Dr. " + vetNom, subject, content);
        } catch (Exception e) {
            System.err.println("❌ Erreur email vet: " + e.getMessage());
        }
    }

    public void notifyClientRdvStatus(String clientEmail, String status, String vetNom, String description) {
        try {
            boolean confirme = "CONFIRME".equals(status);

            String couleurHeader = confirme ? "#22c55e" : "#ef4444";
            String couleurBorder = confirme ? "#22c55e" : "#ef4444";
            String couleurBg     = confirme ? "#f0fdf4" : "#fef2f2";
            String emoji         = confirme ? "✅" : "❌";
            String statusFr      = confirme ? "CONFIRMÉ" : "ANNULÉ";
            String message       = confirme
                    ? "Votre rendez-vous a été <b style='color:#22c55e;'>confirmé</b> ! Nous vous attendons à la clinique."
                    : "Votre rendez-vous a été <b style='color:#ef4444;'>annulé</b>. Vous pouvez reprendre un nouveau rendez-vous.";

            String subject = emoji + " RDV " + statusFr + " - FurHope";
            String content =
                    "<div style='margin:0;padding:0;background-color:#f9fafb;font-family:Arial,sans-serif;'>" +

                            // Header
                            "<div style='background:" + couleurHeader + ";padding:40px 30px;text-align:center;'>" +
                            "<h1 style='color:white;margin:0;font-size:28px;'>🐾 FurHope</h1>" +
                            "<p style='color:white;margin:8px 0 0;font-size:15px;opacity:0.9;'>Clinique Vétérinaire</p>" +
                            "</div>" +


                            "<div style='text-align:center;background:white;padding:30px;'>" +
                            "<div style='display:inline-block;background:" + couleurBg + ";border:2px solid " + couleurBorder + ";" +
                            "border-radius:50px;padding:12px 30px;'>" +
                            "<span style='color:" + couleurHeader + ";font-size:18px;font-weight:bold;'>" +
                            emoji + " Rendez-vous " + statusFr +
                            "</span></div>" +
                            "</div>" +


                            "<div style='background:white;max-width:600px;margin:0 auto;padding:10px 30px 40px;'>" +
                            "<p style='color:#374151;font-size:15px;'>" + message + "</p>" +

                            // Info card
                            "<div style='background:" + couleurBg + ";border-left:4px solid " + couleurBorder + ";" +
                            "border-radius:8px;padding:20px;margin:24px 0;'>" +
                            "<table style='width:100%;border-collapse:collapse;'>" +
                            "<tr>" +
                            "<td style='padding:10px;font-weight:bold;color:#374151;width:40%;'>👨‍⚕️ Vétérinaire</td>" +
                            "<td style='padding:10px;color:#1f2937;'>Dr. " + vetNom + "</td>" +
                            "</tr>" +
                            "<tr style='background:white;'>" +
                            "<td style='padding:10px;font-weight:bold;color:#374151;'>📝 Raison de la visite </td>" +
                            "<td style='padding:10px;color:#1f2937;'>" + description + "</td>" +
                            "</tr>" +
                            "</table>" +
                            "</div>" +

                            (confirme ?
                                    "<div style='background:#f0fdf4;border-radius:8px;padding:16px;margin-top:20px;text-align:center;'>" +
                                            "<p style='color:#16a34a;margin:0;font-size:14px;'>💡 Pensez à venir avec le carnet de santé de votre animal.</p>" +
                                            "</div>" :
                                    "<div style='background:#fef2f2;border-radius:8px;padding:16px;margin-top:20px;text-align:center;'>" +
                                            "<p style='color:#dc2626;margin:0;font-size:14px;'>💡 Vous pouvez reprendre un nouveau rendez-vous depuis votre espace client.</p>" +
                                            "</div>") +

                            // Footer
                            "<hr style='border:none;border-top:1px solid #f3f4f6;margin:30px 0;'/>" +
                            "<p style='color:#9ca3af;font-size:12px;text-align:center;'>" +
                            "🐾 FurHope — <br/>Cet email est automatique, merci de ne pas y répondre.</p>" +
                            "</div></div>";

            sendEmail(clientEmail, "Client", subject, content);
        } catch (Exception e) {
            System.err.println("❌ Erreur email client: " + e.getMessage());
        }
    }

    private String[] parseDescription(String description) {

        String animalNom  = "";
        String animalType = "";
        String date       = "";
        String heure      = "";
        String motif      = description;

        try {

            int start = description.indexOf("[") + 1;
            int end   = description.indexOf("]");
            if (start > 0 && end > start) {
                String animalPart = description.substring(start, end).trim();

                animalPart = animalPart.replaceAll("[^\\p{L}\\p{N}\\s().]", "").trim();

                if (animalPart.contains("(")) {
                    animalNom  = animalPart.substring(0, animalPart.indexOf("(")).trim();
                    animalType = animalPart.substring(
                            animalPart.indexOf("(") + 1,
                            animalPart.indexOf(")")
                    ).trim();
                }
            }


            int start2 = description.indexOf("[", end + 1) + 1;
            int end2   = description.indexOf("]", end + 1);
            if (start2 > 0 && end2 > start2) {
                String datePart = description.substring(start2, end2).trim();

                datePart = datePart.replaceAll("[^\\p{L}\\p{N}\\s:.-]", "").trim();

                if (datePart.contains(" ")) {
                    date  = datePart.substring(0, datePart.lastIndexOf(" ")).trim();
                    heure = datePart.substring(datePart.lastIndexOf(" ") + 1).trim();
                } else {
                    date = datePart;
                }
            }


            int lastBracket = description.lastIndexOf("]");
            if (lastBracket >= 0 && lastBracket < description.length() - 1) {
                motif = description.substring(lastBracket + 1).trim();
            }

        } catch (Exception ignored) {}

        return new String[]{animalNom, animalType, date, heure, motif};
    }
}