package com.esprit.Services;

import com.esprit.config.ConfigManager;
import com.esprit.entities.adoptionRequest;
import com.esprit.entities.animal;
import com.esprit.entities.User;
import com.esprit.utils.Session;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Service pour envoyer des emails via Brevo (anciennement Sendinblue)
 */
public class BrevoService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private String apiKey;

    public BrevoService() {
        this.apiKey = ConfigManager.get("brevo.api.key");

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("⚠️ Clé API Brevo non configurée!");
            System.err.println("   Assurez-vous que 'brevo.api.key' est définie dans config.properties");
        }
    }

    /**
     * 📧 Envoyer un email d'approbation
     */
    public boolean sendApprovalEmail(adoptionRequest request) {
        String recipientEmail = getClientEmail(request);
        if (recipientEmail == null || recipientEmail.isEmpty()) {
            System.err.println("❌ Email du demandeur non trouvé");
            return false;
        }

        String subject = "🎉 Félicitations ! Votre demande d'adoption a été APPROUVÉE !";
        String htmlContent = buildApprovalEmailHtml(request);

        // ✅ Utiliser l'email de l'admin connecté comme expéditeur
        String fromEmail = Session.getUserEmail();
        String fromName  = Session.getUserName();

        return sendEmail(recipientEmail, subject, htmlContent, fromEmail, fromName);
    }

    /**
     * 📧 Envoyer un email de rejet
     */
    public boolean sendDeclineEmail(adoptionRequest request) {
        String recipientEmail = getClientEmail(request);
        if (recipientEmail == null || recipientEmail.isEmpty()) {
            System.err.println("❌ Email du demandeur non trouvé");
            return false;
        }

        String subject = "📋 Mise à jour sur votre demande d'adoption";
        String htmlContent = buildDeclineEmailHtml(request);

        // ✅ Utiliser l'email de l'admin connecté comme expéditeur
        String fromEmail = Session.getUserEmail();
        String fromName  = Session.getUserName();

        return sendEmail(recipientEmail, subject, htmlContent, fromEmail, fromName);
    }

    /**
     * 📧 Envoyer un email générique
     */
    private boolean sendEmail(String toEmail, String subject, String htmlContent,
                              String fromEmail, String fromName) {
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("❌ Clé API Brevo non configurée");
            return false;
        }

        // Fallback si session vide
        if (fromEmail == null || fromEmail.isEmpty()) {
            fromEmail = ConfigManager.get("email.from");
        }
        if (fromName == null || fromName.isEmpty()) {
            fromName = ConfigManager.get("app.name");
        }

        try {
            JSONObject requestBody = new JSONObject();

            // ✅ Expéditeur = admin connecté
            JSONObject sender = new JSONObject();
            sender.put("name", fromName);
            sender.put("email", fromEmail);
            requestBody.put("sender", sender);

            // Destinataire
            JSONObject contact = new JSONObject();
            contact.put("email", toEmail);
            requestBody.put("to", new org.json.JSONArray().put(contact));

            // Sujet et contenu
            requestBody.put("subject", subject);
            requestBody.put("htmlContent", htmlContent);

            return sendHttpRequest(requestBody.toString());

        } catch (Exception e) {
            System.err.println("❌ Erreur création email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 🌐 Envoyer la requête HTTP à Brevo
     */
    private boolean sendHttpRequest(String requestBody) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(BREVO_API_URL).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("api-key", apiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            System.out.println("📡 Réponse Brevo: " + responseCode);

            if (responseCode == 201 || responseCode == 200) {
                System.out.println("✅ Email envoyé avec succès !");
                return true;
            } else {
                String errorMessage = readErrorResponse(connection);
                System.err.println("❌ Erreur Brevo: " + responseCode);
                System.err.println("   " + errorMessage);
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 📖 Lire la réponse d'erreur
     */
    private String readErrorResponse(HttpURLConnection connection) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getErrorStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * 🎨 HTML email d'approbation
     */
    private String buildApprovalEmailHtml(adoptionRequest request) {
        animal animal = request.getAnimal();
        User client = request.getClient();

        // Infos admin expéditeur
        String adminName  = Session.getUserName()  != null ? Session.getUserName()  : "L'équipe Animal Shelter";
        String adminEmail = Session.getUserEmail() != null ? Session.getUserEmail() : "contact@animalshelter.com";

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head><meta charset='UTF-8'>\n" +
                "  <style>\n" +
                "    body { font-family: Arial, sans-serif; color: #333; }\n" +
                "    .container { max-width: 600px; margin: 0 auto; padding: 20px; }\n" +
                "    .header { background-color: #27ae60; color: white; padding: 20px; border-radius: 10px; text-align: center; }\n" +
                "    .content { padding: 20px; background-color: #f9f9f9; margin: 20px 0; border-radius: 10px; }\n" +
                "    .animal-info { background-color: #e8f5e9; padding: 15px; border-radius: 8px; margin: 15px 0; }\n" +
                "    .admin-info { background-color: #eaf4fb; padding: 10px 15px; border-radius: 8px; margin: 15px 0; font-size: 13px; }\n" +
                "    .footer { text-align: center; color: #999; font-size: 12px; margin-top: 20px; }\n" +
                "    .button { display: inline-block; padding: 12px 30px; background-color: #27ae60; color: white; text-decoration: none; border-radius: 5px; margin-top: 20px; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class='container'>\n" +
                "    <div class='header'>\n" +
                "      <h1>🎉 Félicitations ! 🎉</h1>\n" +
                "      <p>Votre demande d'adoption a été APPROUVÉE !</p>\n" +
                "    </div>\n" +
                "    <div class='content'>\n" +
                "      <h2>Bonjour " + (client != null ? client.getName() : "Demandeur") + ",</h2>\n" +
                "      <p>Nous avons le plaisir de vous informer que votre demande d'adoption pour <strong>" +
                (animal != null ? animal.getName() : "l'animal") + "</strong> a été <strong style='color:#27ae60;'>APPROUVÉE</strong> ! 🐾</p>\n" +
                "      <div class='animal-info'>\n" +
                "        <h3>Informations sur votre animal :</h3>\n" +
                "        <p><strong>Nom :</strong> "     + (animal != null ? animal.getName()    : "-") + "</p>\n" +
                "        <p><strong>Espèce :</strong> "  + (animal != null ? animal.getSpecies() : "-") + "</p>\n" +
                "        <p><strong>Race :</strong> "    + (animal != null ? animal.getBreed()   : "-") + "</p>\n" +
                "        <p><strong>Âge :</strong> "     + (animal != null ? animal.getAge() + " ans" : "-") + "</p>\n" +
                "      </div>\n" +
                "      <div class='admin-info'>\n" +
                "        <p>✅ Décision prise par : <strong>" + adminName + "</strong> (" + adminEmail + ")</p>\n" +
                "      </div>\n" +
                "      <h3>Prochaines étapes :</h3>\n" +
                "      <ol>\n" +
                "        <li>Contactez le refuge pour finaliser les démarches administratives</li>\n" +
                "        <li>Préparez votre maison pour l'arrivée de votre nouvel ami</li>\n" +
                "        <li>Fixez une date et une heure pour la remise de l'animal</li>\n" +
                "      </ol>\n" +
                "      <p style='margin-top:30px;'>\n" +
                "        <a href='mailto:" + adminEmail + "' class='button'>Contacter l'équipe</a>\n" +
                "      </p>\n" +
                "    </div>\n" +
                "    <div class='footer'>\n" +
                "      <p>© 2024 Animal Shelter - Tous droits réservés</p>\n" +
                "      <p>Cet email a été envoyé automatiquement par " + adminName + ".</p>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body></html>";
    }

    /**
     * 🎨 HTML email de rejet
     */
    private String buildDeclineEmailHtml(adoptionRequest request) {
        animal animal = request.getAnimal();
        User client = request.getClient();

        String adminName  = Session.getUserName()  != null ? Session.getUserName()  : "L'équipe Animal Shelter";
        String adminEmail = Session.getUserEmail() != null ? Session.getUserEmail() : "contact@animalshelter.com";

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head><meta charset='UTF-8'>\n" +
                "  <style>\n" +
                "    body { font-family: Arial, sans-serif; color: #333; }\n" +
                "    .container { max-width: 600px; margin: 0 auto; padding: 20px; }\n" +
                "    .header { background-color: #e74c3c; color: white; padding: 20px; border-radius: 10px; text-align: center; }\n" +
                "    .content { padding: 20px; background-color: #f9f9f9; margin: 20px 0; border-radius: 10px; }\n" +
                "    .animal-info { background-color: #fef5e7; padding: 15px; border-radius: 8px; margin: 15px 0; }\n" +
                "    .admin-info { background-color: #eaf4fb; padding: 10px 15px; border-radius: 8px; margin: 15px 0; font-size: 13px; }\n" +
                "    .footer { text-align: center; color: #999; font-size: 12px; margin-top: 20px; }\n" +
                "    .button { display: inline-block; padding: 12px 30px; background-color: #3498db; color: white; text-decoration: none; border-radius: 5px; margin-top: 20px; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class='container'>\n" +
                "    <div class='header'>\n" +
                "      <h1>📋 Mise à jour sur votre demande</h1>\n" +
                "    </div>\n" +
                "    <div class='content'>\n" +
                "      <h2>Bonjour " + (client != null ? client.getName() : "Demandeur") + ",</h2>\n" +
                "      <p>Nous vous informons que votre demande d'adoption pour <strong>" +
                (animal != null ? animal.getName() : "l'animal") + "</strong> a été <strong style='color:#e74c3c;'>DÉCLINÉE</strong>. 😔</p>\n" +
                "      <div class='animal-info'>\n" +
                "        <h3>Animal concerné :</h3>\n" +
                "        <p><strong>Nom :</strong> "    + (animal != null ? animal.getName()    : "-") + "</p>\n" +
                "        <p><strong>Espèce :</strong> " + (animal != null ? animal.getSpecies() : "-") + "</p>\n" +
                "      </div>\n" +
                "      <div class='admin-info'>\n" +
                "        <p>❌ Décision prise par : <strong>" + adminName + "</strong> (" + adminEmail + ")</p>\n" +
                "      </div>\n" +
                "      <h3>Que faire maintenant ?</h3>\n" +
                "      <ul>\n" +
                "        <li>Consultez notre liste complète d'animaux disponibles</li>\n" +
                "        <li>Vous pouvez soumettre une nouvelle demande pour un autre animal</li>\n" +
                "        <li>Contactez-nous pour discuter et améliorer votre profil</li>\n" +
                "      </ul>\n" +
                "      <p style='margin-top:30px;'>\n" +
                "        <a href='mailto:" + adminEmail + "' class='button'>Contacter l'équipe</a>\n" +
                "      </p>\n" +
                "    </div>\n" +
                "    <div class='footer'>\n" +
                "      <p>© 2024 Animal Shelter - Tous droits réservés</p>\n" +
                "      <p>Cet email a été envoyé automatiquement par " + adminName + ".</p>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body></html>";
    }

    /**
     * 🔍 Récupérer l'email du client destinataire
     */
    private String getClientEmail(adoptionRequest request) {
        if (request == null || request.getClient() == null) return null;
        return request.getClient().getEmail();
    }
}
