package controllers;
import utils.Config;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import okhttp3.*;
import com.google.gson.*;
import services.ServiceRendezvous;
import utils.SessionManager;
import utils.ViewNavigator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class StatsDashboardController {

    @FXML private WebView webView;
    @FXML private Label welcomeLabel;

    private static final String GROQ_KEY = Config.get("GROQ_KEY");
    private final ServiceRendezvous serviceRdv = new ServiceRendezvous();
    private WebEngine engine;

    @FXML
    public void initialize() {
        welcomeLabel.setText("Bonjour Dr. " + SessionManager.getUserNom() + " 👋");
        engine = webView.getEngine();
        loadDashboard();
    }

    private void loadDashboard() {
        try {
            int vetId = SessionManager.getUserId();


            Map<String, Integer> statsCeMois  = serviceRdv.getStatsMoisActuel(vetId);
            Map<String, Integer> statsMoisPrec = serviceRdv.getStatsMoisPrecedent(vetId);
            Map<String, Integer> rdvParJour   = serviceRdv.getRdvParJourCeMois(vetId);

            int confirme  = statsCeMois.getOrDefault("CONFIRME", 0);
            int annule    = statsCeMois.getOrDefault("ANNULE", 0);
            int attente   = statsCeMois.getOrDefault("EN_ATTENTE", 0);
            int termine   = statsCeMois.getOrDefault("TERMINE", 0);
            int totalCeMois  = confirme + annule + attente + termine;
            int totalMoisPrec = statsMoisPrec.values().stream().mapToInt(i -> i).sum();


            StringBuilder rdvLabels = new StringBuilder();
            StringBuilder rdvValues = new StringBuilder();
            for (Map.Entry<String, Integer> e : rdvParJour.entrySet()) {
                rdvLabels.append("'").append(e.getKey().substring(8)).append("',");
                rdvValues.append(e.getValue()).append(",");
            }


            StringBuilder reviewLabels = new StringBuilder();
            StringBuilder reviewValues = new StringBuilder();
            StringBuilder compareNames = new StringBuilder();
            StringBuilder compareValues = new StringBuilder();

            Connection conn = utils.MyDatabase.getInstance().getConnection();

            PreparedStatement ps1 = conn.prepareStatement(
                    "SELECT DATE(created_at) as jour, AVG(rating) as moyenne " +
                            "FROM review WHERE vet_id = ? " +
                            "GROUP BY DATE(created_at) ORDER BY jour ASC"
            );
            ps1.setInt(1, vetId);
            ResultSet rs1 = ps1.executeQuery();
            while (rs1.next()) {
                reviewLabels.append("'").append(rs1.getString("jour")).append("',");
                reviewValues.append(
                        Math.round(rs1.getDouble("moyenne") * 10.0) / 10.0
                ).append(",");
            }
            PreparedStatement ps2 = conn.prepareStatement(
                    "SELECT u.first_name, AVG(rv.rating) as moyenne " +
                            "FROM review rv JOIN user u ON u.id = rv.vet_id " +
                            "GROUP BY rv.vet_id, u.first_name ORDER BY moyenne DESC"
            );
            ResultSet rs2 = ps2.executeQuery();
            String monPrenom = SessionManager.getUserNom().split(" ")[0];
            StringBuilder compareColors = new StringBuilder();
            while (rs2.next()) {
                String prenom = rs2.getString("first_name");
                double moy = Math.round(rs2.getDouble("moyenne") * 10.0) / 10.0;
                compareNames.append("'Dr. ").append(prenom).append("',");
                compareValues.append(moy).append(",");
                if (prenom.equalsIgnoreCase(monPrenom)) {
                    compareColors.append("'#f97316',");
                } else {
                    compareColors.append("'#3b82f6',");
                }
            }
            PreparedStatement ps3 = conn.prepareStatement(
                    "SELECT AVG(rating) as moy, COUNT(*) as nb FROM review WHERE vet_id = ?"
            );
            ps3.setInt(1, vetId);
            ResultSet rs3 = ps3.executeQuery();
            double noteMoyenne = 0;
            int nbAvis = 0;
            if (rs3.next()) {
                noteMoyenne = Math.round(rs3.getDouble("moy") * 10.0) / 10.0;
                nbAvis = rs3.getInt("nb");
            }

            // Evolution vs mois précédent
            int diff = totalCeMois - totalMoisPrec;
            String evolution = diff > 0 ? "↑ +" + diff : diff < 0 ? "↓ " + diff : "→ Stable";
            String evolutionColor = diff > 0 ? "#22c55e" : diff < 0 ? "#ef4444" : "#f59e0b";

            // ✅ Générer HTML
            final String html = buildHtml(
                    confirme, annule, attente, termine,
                    totalCeMois, evolution, evolutionColor,
                    noteMoyenne, nbAvis,
                    rdvLabels.toString(), rdvValues.toString(),
                    reviewLabels.toString(), reviewValues.toString(),
                    compareNames.toString(), compareValues.toString(),
                    compareColors.toString()
            );

            Platform.runLater(() -> {
                engine.loadContent(html);
                // ✅ Charger analyse IA après chargement page
                engine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
                    if (newState == Worker.State.SUCCEEDED) {
                        loadAiConseil(confirme, annule, attente, totalMoisPrec, vetId);
                    }
                });
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildHtml(
            int confirme, int annule, int attente, int termine,
            int total, String evolution, String evolutionColor,
            double noteMoy, int nbAvis,
            String rdvLabels, String rdvValues,
            String reviewLabels, String reviewValues,
            String compareNames, String compareValues, String compareColors
    ) {
        String etoiles = "⭐".repeat((int) Math.round(noteMoy)) +
                "☆".repeat(5 - (int) Math.round(noteMoy));

        return "<!DOCTYPE html><html><head>" +
                "<meta charset='UTF-8'>" +
                "<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>" +
                "<style>" +
                "* { margin:0; padding:0; box-sizing:border-box; font-family:'Segoe UI',sans-serif; }" +
                "body { background:#0f172a; color:#e2e8f0; padding:24px; }" +
                ".grid { display:grid; grid-template-columns:repeat(4,1fr); gap:16px; margin-bottom:24px; }" +
                ".card { background:#1e293b; border-radius:16px; padding:20px; border:1px solid #334155; }" +
                ".card-title { font-size:12px; color:#94a3b8; text-transform:uppercase; letter-spacing:1px; margin-bottom:8px; }" +
                ".card-value { font-size:32px; font-weight:800; margin-bottom:4px; }" +
                ".card-sub { font-size:12px; color:#64748b; }" +
                ".chart-card { background:#1e293b; border-radius:16px; padding:24px; border:1px solid #334155; margin-bottom:20px; }" +
                ".chart-title { font-size:16px; font-weight:700; margin-bottom:20px; color:#f1f5f9; }" +
                ".ai-card { background:linear-gradient(135deg,#1e293b,#0f172a); border-radius:16px; padding:24px;" +
                "           border:1px solid #7c3aed; margin-bottom:20px; }" +
                ".ai-title { font-size:16px; font-weight:700; color:#a78bfa; margin-bottom:12px; }" +
                "#aiText { font-size:14px; color:#cbd5e1; line-height:1.7; }" +
                ".stars { font-size:20px; margin-bottom:4px; }" +
                "</style></head><body>" +

                // ✅ Cartes stats
                "<div class='grid'>" +
                "<div class='card'><div class='card-title'>✅ Confirmés</div>" +
                "<div class='card-value' style='color:#22c55e;'>" + confirme + "</div>" +
                "<div class='card-sub'>ce mois</div></div>" +

                "<div class='card'><div class='card-title'>❌ Annulés</div>" +
                "<div class='card-value' style='color:#ef4444;'>" + annule + "</div>" +
                "<div class='card-sub'>ce mois</div></div>" +

                "<div class='card'><div class='card-title'>⏳ En attente</div>" +
                "<div class='card-value' style='color:#f59e0b;'>" + attente + "</div>" +
                "<div class='card-sub'>ce mois</div></div>" +

                "<div class='card'><div class='card-title'>📈 Évolution</div>" +
                "<div class='card-value' style='color:" + evolutionColor + ";font-size:22px;'>" + evolution + "</div>" +
                "<div class='card-sub'>vs mois précédent</div></div>" +
                "</div>" +

                // ✅ Note moyenne
                "<div class='grid' style='grid-template-columns:1fr 1fr;'>" +
                "<div class='card'><div class='card-title'>⭐ Note moyenne</div>" +
                "<div class='stars'>" + etoiles + "</div>" +
                "<div class='card-value' style='color:#f97316;'>" + noteMoy + "<span style='font-size:16px;color:#64748b;'>/5</span></div>" +
                "<div class='card-sub'>" + nbAvis + " avis clients</div></div>" +

                "<div class='card'><div class='card-title'>🏁 Terminés</div>" +
                "<div class='card-value' style='color:#8b5cf6;'>" + termine + "</div>" +
                "<div class='card-sub'>consultations ce mois</div></div>" +
                "</div>" +

                // ✅ Analyse IA
                "<div class='ai-card'>" +
                "<div class='ai-title'>🤖 Analyse IA — Conseil personnalisé</div>" +
                "<div id='aiText'>⏳ Analyse en cours...</div>" +
                "</div>" +


                "<div class='chart-card'>" +
                "<div class='chart-title'>📅 Rendez-vous par jour — ce mois</div>" +
                "<canvas id='rdvChart' height='100'></canvas></div>" +


                "<div class='chart-card'>" +
                "<div class='chart-title'>⭐ Évolution de vos notes clients</div>" +
                "<canvas id='reviewChart' height='100'></canvas></div>" +

                "<div class='chart-card'>" +
                "<div class='chart-title'>📊 Comparaison avec les autres vétérinaires</div>" +
                "<canvas id='compareChart' height='100'></canvas></div>" +

                "<script>" +

                "new Chart(document.getElementById('rdvChart'),{" +
                "type:'line'," +
                "data:{labels:[" + rdvLabels + "]," +
                "datasets:[{label:'RDV',data:[" + rdvValues + "]," +
                "borderColor:'#f97316',backgroundColor:'rgba(249,115,22,0.1)'," +
                "borderWidth:3,fill:true,tension:0.4,pointBackgroundColor:'#f97316'," +
                "pointRadius:5,pointHoverRadius:8}]}," +
                "options:{responsive:true,plugins:{legend:{labels:{color:'#94a3b8'}}}," +
                "scales:{x:{ticks:{color:'#94a3b8'},grid:{color:'#1e293b'}}," +
                "y:{ticks:{color:'#94a3b8'},grid:{color:'#334155'},min:0}}}});" +


                "new Chart(document.getElementById('reviewChart'),{" +
                "type:'line'," +
                "data:{labels:[" + reviewLabels + "]," +
                "datasets:[{label:'Note /5',data:[" + reviewValues + "]," +
                "borderColor:'#eab308',backgroundColor:'rgba(234,179,8,0.1)'," +
                "borderWidth:3,fill:true,tension:0.4,pointBackgroundColor:'#eab308'," +
                "pointRadius:6,pointHoverRadius:9}]}," +
                "options:{responsive:true,plugins:{legend:{labels:{color:'#94a3b8'}}}," +
                "scales:{x:{ticks:{color:'#94a3b8'},grid:{color:'#334155'}}," +
                "y:{min:0,max:5,ticks:{color:'#94a3b8',stepSize:1},grid:{color:'#334155'}}}}});" +


                "new Chart(document.getElementById('compareChart'),{" +
                "type:'bar'," +
                "data:{labels:[" + compareNames + "]," +
                "datasets:[{label:'Note moyenne /5',data:[" + compareValues + "]," +
                "backgroundColor:[" + compareColors + "]," +
                "borderRadius:8,borderSkipped:false}]}," +
                "options:{responsive:true,plugins:{legend:{labels:{color:'#94a3b8'}}}," +
                "scales:{x:{ticks:{color:'#94a3b8'},grid:{color:'#334155'}}," +
                "y:{min:0,max:5,ticks:{color:'#94a3b8',stepSize:1},grid:{color:'#334155'}}}}});" +
                "</script></body></html>";
    }

    private void loadAiConseil(int confirme, int annule, int attente, int totalMoisPrec, int vetId) {
        CompletableFuture.runAsync(() -> {
            try {

                Connection conn = utils.MyDatabase.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT rating, commentaire FROM review WHERE vet_id = ?"
                );
                ps.setInt(1, vetId);
                ResultSet rs = ps.executeQuery();

                StringBuilder commentaires = new StringBuilder();
                int count = 0;
                while (rs.next()) {
                    String c = rs.getString("commentaire");
                    if (c != null && !c.trim().isEmpty()) {
                        commentaires.append("Note:").append(rs.getInt("rating"))
                                .append("/5 — ").append(c).append(". ");
                        count++;
                    }
                }

                String prompt;
                if (count == 0) {
                    prompt = "Tu es un assistant pour vétérinaires. " +
                            "Statistiques du Dr. " + SessionManager.getUserNom() + " ce mois : " +
                            "Confirmés=" + confirme + ", Annulés=" + annule + ", En attente=" + attente +
                            ". Mois précédent total=" + totalMoisPrec +
                            ". Donne 2-3 conseils professionnels pour améliorer sa performance. En français.";
                } else {
                    prompt = "Tu es un expert satisfaction client vétérinaire. " +
                            "Avis clients du Dr. " + SessionManager.getUserNom() + " : " + commentaires +
                            "Stats ce mois: Confirmés=" + confirme + ", Annulés=" + annule +
                            ". Donne: 1) Points forts 2) Points à améliorer 3) Conseil concret. " +
                            "Max 4 phrases. En français.";
                }

                String conseil = callGroqApi(prompt, 400);


                String js = "document.getElementById('aiText').innerHTML = '" +
                        conseil.replace("'", "\\'")
                                .replace("\n", "<br/>")
                                .replace("\"", "\\\"") + "';";

                Platform.runLater(() -> engine.executeScript(js));

            } catch (Exception e) {
                Platform.runLater(() ->
                        engine.executeScript(
                                "document.getElementById('aiText').innerHTML = '❌ Erreur IA : " +
                                        e.getMessage().replace("'", "") + "';"
                        )
                );
            }
        });
    }

    private String callGroqApi(String prompt, int maxTokens) throws Exception {
        OkHttpClient client = new OkHttpClient();

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject body = new JsonObject();
        body.addProperty("model", "llama3-8b-8192");
        body.addProperty("max_tokens", maxTokens);
        body.add("messages", messages);

        RequestBody requestBody = RequestBody.create(
                body.toString(), MediaType.get("application/json")
        );

        Request request = new Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + GROQ_KEY)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build();

        try (okhttp3.Response response = client.newCall(request).execute()) {
            String json = response.body().string();
            JsonObject result = JsonParser.parseString(json).getAsJsonObject();
            if (result.has("error")) {
                return "❌ " + result.getAsJsonObject("error")
                        .get("message").getAsString();
            }
            return result.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        }
    }

    @FXML
    private void goDisponibilites(ActionEvent event) {
        ViewNavigator.goTo(event, "/DisponibiliteForm.fxml", "Mes Disponibilités");
    }

    @FXML
    private void goRendezvous(ActionEvent event) {
        ViewNavigator.goTo(event, "/RendezvousList.fxml", "Gérer Rendez-vous");
    }
    @FXML
    private void goBackAccueil(ActionEvent event) {
        ViewNavigator.goTo(event, "/accueil.fxml", "FurHope");
    }

    @FXML
    private void logout(ActionEvent event) {
        SessionManager.logout();
        ViewNavigator.goTo(event, "/Home.fxml", "Clinique Vétérinaire");
    }
}