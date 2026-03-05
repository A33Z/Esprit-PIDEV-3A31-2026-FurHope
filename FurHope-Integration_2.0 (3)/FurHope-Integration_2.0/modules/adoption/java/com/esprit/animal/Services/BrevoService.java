package com.esprit.animal.Services;

import com.esprit.animal.config.ConfigManager;
import com.esprit.animal.entities.adoptionRequest;
import com.esprit.animal.entities.animal;
import com.esprit.animal.entities.User;
import com.esprit.animal.utils.Session;
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
            System.err.println("âš ï¸ ClÃ© API Brevo non configurÃ©e!");
            System.err.println("   Assurez-vous que 'brevo.api.key' est dÃ©finie dans config.properties");
        }
    }

    /**
     * ðŸ“§ Envoyer un email d'approbation
     */
    public boolean sendApprovalEmail(adoptionRequest request) {
        String recipientEmail = getClientEmail(request);
        if (recipientEmail == null || recipientEmail.isEmpty()) {
            System.err.println("âŒ Email du demandeur non trouvÃ©");
            return false;
        }

        String subject = "ðŸŽ‰ FÃ©licitations ! Votre demande d'adoption a Ã©tÃ© APPROUVÃ‰E !";
        String htmlContent = buildApprovalEmailHtml(request);

        // âœ… Utiliser l'email de l'admin connectÃ© comme expÃ©diteur
        String fromEmail = Session.getUserEmail();
        String fromName  = Session.getUserName();

        return sendEmail(recipientEmail, subject, htmlContent, fromEmail, fromName);
    }

    /**
     * ðŸ“§ Envoyer un email de rejet
     */
    public boolean sendDeclineEmail(adoptionRequest request) {
        String recipientEmail = getClientEmail(request);
        if (recipientEmail == null || recipientEmail.isEmpty()) {
            System.err.println("âŒ Email du demandeur non trouvÃ©");
            return false;
        }

        String subject = "ðŸ“‹ Mise Ã  jour sur votre demande d'adoption";
        String htmlContent = buildDeclineEmailHtml(request);

        // âœ… Utiliser l'email de l'admin connectÃ© comme expÃ©diteur
        String fromEmail = Session.getUserEmail();
        String fromName  = Session.getUserName();

        return sendEmail(recipientEmail, subject, htmlContent, fromEmail, fromName);
    }

    /**
     * ðŸ“§ Envoyer un email gÃ©nÃ©rique
     */
    private boolean sendEmail(String toEmail, String subject, String htmlContent,
                              String fromEmail, String fromName) {
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("âŒ ClÃ© API Brevo non configurÃ©e");
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

            // âœ… ExpÃ©diteur = admin connectÃ©
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
            System.err.println("âŒ Erreur crÃ©ation email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ðŸŒ Envoyer la requÃªte HTTP Ã  Brevo
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
            System.out.println("ðŸ“¡ RÃ©ponse Brevo: " + responseCode);

            if (responseCode == 201 || responseCode == 200) {
                System.out.println("âœ… Email envoyÃ© avec succÃ¨s !");
                return true;
            } else {
                String errorMessage = readErrorResponse(connection);
                System.err.println("âŒ Erreur Brevo: " + responseCode);
                System.err.println("   " + errorMessage);
                return false;
            }

        } catch (Exception e) {
            System.err.println("âŒ Erreur envoi email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ðŸ“– Lire la rÃ©ponse d'erreur
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
     * ðŸŽ¨ HTML email d'approbation
     */
    private String buildApprovalEmailHtml(adoptionRequest request) {
        animal animal = request.getAnimal();
        User client = request.getClient();

        // Infos admin expÃ©diteur
        String adminName  = Session.getUserName()  != null ? Session.getUserName()  : "L'Ã©quipe Animal Shelter";
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
                "      <h1>ðŸŽ‰ FÃ©licitations ! ðŸŽ‰</h1>\n" +
                "      <p>Votre demande d'adoption a Ã©tÃ© APPROUVÃ‰E !</p>\n" +
                "    </div>\n" +
                "    <div class='content'>\n" +
                "      <h2>Bonjour " + (client != null ? client.getName() : "Demandeur") + ",</h2>\n" +
                "      <p>Nous avons le plaisir de vous informer que votre demande d'adoption pour <strong>" +
                (animal != null ? animal.getName() : "l'animal") + "</strong> a Ã©tÃ© <strong style='color:#27ae60;'>APPROUVÃ‰E</strong> ! ðŸ¾</p>\n" +
                "      <div class='animal-info'>\n" +
                "        <h3>Informations sur votre animal :</h3>\n" +
                "        <p><strong>Nom :</strong> "     + (animal != null ? animal.getName()    : "-") + "</p>\n" +
                "        <p><strong>EspÃ¨ce :</strong> "  + (animal != null ? animal.getSpecies() : "-") + "</p>\n" +
                "        <p><strong>Race :</strong> "    + (animal != null ? animal.getBreed()   : "-") + "</p>\n" +
                "        <p><strong>Ã‚ge :</strong> "     + (animal != null ? animal.getAge() + " ans" : "-") + "</p>\n" +
                "      </div>\n" +
                "      <div class='admin-info'>\n" +
                "        <p>âœ… DÃ©cision prise par : <strong>" + adminName + "</strong> (" + adminEmail + ")</p>\n" +
                "      </div>\n" +
                "      <h3>Prochaines Ã©tapes :</h3>\n" +
                "      <ol>\n" +
                "        <li>Contactez le refuge pour finaliser les dÃ©marches administratives</li>\n" +
                "        <li>PrÃ©parez votre maison pour l'arrivÃ©e de votre nouvel ami</li>\n" +
                "        <li>Fixez une date et une heure pour la remise de l'animal</li>\n" +
                "      </ol>\n" +
                "      <p style='margin-top:30px;'>\n" +
                "        <a href='mailto:" + adminEmail + "' class='button'>Contacter l'Ã©quipe</a>\n" +
                "      </p>\n" +
                "    </div>\n" +
                "    <div class='footer'>\n" +
                "      <p>Â© 2024 Animal Shelter - Tous droits rÃ©servÃ©s</p>\n" +
                "      <p>Cet email a Ã©tÃ© envoyÃ© automatiquement par " + adminName + ".</p>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body></html>";
    }

    /**
     * ðŸŽ¨ HTML email de rejet
     */
    private String buildDeclineEmailHtml(adoptionRequest request) {
        animal animal = request.getAnimal();
        User client = request.getClient();

        String adminName  = Session.getUserName()  != null ? Session.getUserName()  : "L'Ã©quipe Animal Shelter";
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
                "      <h1>ðŸ“‹ Mise Ã  jour sur votre demande</h1>\n" +
                "    </div>\n" +
                "    <div class='content'>\n" +
                "      <h2>Bonjour " + (client != null ? client.getName() : "Demandeur") + ",</h2>\n" +
                "      <p>Nous vous informons que votre demande d'adoption pour <strong>" +
                (animal != null ? animal.getName() : "l'animal") + "</strong> a Ã©tÃ© <strong style='color:#e74c3c;'>DÃ‰CLINÃ‰E</strong>. ðŸ˜”</p>\n" +
                "      <div class='animal-info'>\n" +
                "        <h3>Animal concernÃ© :</h3>\n" +
                "        <p><strong>Nom :</strong> "    + (animal != null ? animal.getName()    : "-") + "</p>\n" +
                "        <p><strong>EspÃ¨ce :</strong> " + (animal != null ? animal.getSpecies() : "-") + "</p>\n" +
                "      </div>\n" +
                "      <div class='admin-info'>\n" +
                "        <p>âŒ DÃ©cision prise par : <strong>" + adminName + "</strong> (" + adminEmail + ")</p>\n" +
                "      </div>\n" +
                "      <h3>Que faire maintenant ?</h3>\n" +
                "      <ul>\n" +
                "        <li>Consultez notre liste complÃ¨te d'animaux disponibles</li>\n" +
                "        <li>Vous pouvez soumettre une nouvelle demande pour un autre animal</li>\n" +
                "        <li>Contactez-nous pour discuter et amÃ©liorer votre profil</li>\n" +
                "      </ul>\n" +
                "      <p style='margin-top:30px;'>\n" +
                "        <a href='mailto:" + adminEmail + "' class='button'>Contacter l'Ã©quipe</a>\n" +
                "      </p>\n" +
                "    </div>\n" +
                "    <div class='footer'>\n" +
                "      <p>Â© 2024 Animal Shelter - Tous droits rÃ©servÃ©s</p>\n" +
                "      <p>Cet email a Ã©tÃ© envoyÃ© automatiquement par " + adminName + ".</p>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body></html>";
    }

    /**
     * ðŸ” RÃ©cupÃ©rer l'email du client destinataire
     */
    private String getClientEmail(adoptionRequest request) {
        if (request == null || request.getClient() == null) return null;
        return request.getClient().getEmail();
    }
}

