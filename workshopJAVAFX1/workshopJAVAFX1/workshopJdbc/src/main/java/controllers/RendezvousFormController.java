package controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Disponibilite;
import model.Rendezvous;
import services.EmailService;
import services.ServiceDisponibilite;
import services.ServiceRendezvous;
import utils.MyDatabase;
import utils.SessionManager;
import utils.ViewNavigator;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RendezvousFormController {

    @FXML private Label titleLabel;
    @FXML private Label vetLabel;
    @FXML private Label errorLabel;
    @FXML private ComboBox<String> disponibiliteBox;
    @FXML private TextField animalNomField;
    @FXML private ComboBox<String> animalTypeBox;
    @FXML private TextArea descriptionArea;
    @FXML private Button saveButton;
    @FXML private TextField numField;

    private final ServiceDisponibilite serviceDispo = new ServiceDisponibilite();
    private final ServiceRendezvous serviceRdv = new ServiceRendezvous();
    private final EmailService emailService = new EmailService();

    private final Map<String, Integer> disponibiliteIds = new HashMap<>();
    private final Map<String, String>  slotStartTimes   = new HashMap<>(); // ✅ Nouveau

    @FXML
    public void initialize() {
        vetLabel.setText("👨‍⚕️ Vétérinaire : " + SessionManager.getSelectedVetNom());
        animalTypeBox.setItems(FXCollections.observableArrayList(
                "🐶 Chien", "🐱 Chat", "🐦 Oiseau",
                "🐹 Hamster", "🐰 Lapin", "🐍 Reptile", "🐾 Autre"
        ));
        loadDisponibilites();
        prefillPhone();
    }

    private void loadDisponibilites() {
        try {
            int vetId = SessionManager.getSelectedVetId();
            List<Disponibilite> dispos = serviceDispo.readByVetId(vetId);
            List<String> takenSlots = serviceRdv.getTakenSlots(vetId);

            for (Disponibilite d : dispos) {
                if (d.getStatut() == Disponibilite.Statut.VALABLE) {
                    LocalDateTime start = d.getStarttime();
                    LocalDateTime end   = d.getEndtime();

                    while (!start.plusMinutes(30).isAfter(end)) {
                        LocalDateTime slotEnd = start.plusMinutes(30);

                        String slotStart = start.toLocalDate() + " " +
                                start.toLocalTime().toString().substring(0, 5);
                        if (!takenSlots.contains(slotStart)) {
                            String label = "📅 " + start.toLocalDate() + "  " +
                                    start.toLocalTime().toString().substring(0, 5) +
                                    " → " + slotEnd.toLocalTime().toString().substring(0, 5);

                            disponibiliteBox.getItems().add(label);
                            disponibiliteIds.put(label, d.getId_disponibilite());
                            slotStartTimes.put(label, slotStart);
                        }

                        start = slotEnd;
                    }
                }
            }

            if (disponibiliteBox.getItems().isEmpty()) {
                disponibiliteBox.setPromptText("❌ Aucun créneau disponible");
                disponibiliteBox.setDisable(true);
            }

        } catch (Exception e) {
            errorLabel.setText("❌ Erreur disponibilités : " + e.getMessage());
        }
    }

    private void prefillPhone() {
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT phone FROM user WHERE id = ?"
            );
            ps.setInt(1, SessionManager.getUserId());
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getString("phone") != null) {
                numField.setText(rs.getString("phone"));
            }
        } catch (Exception ignored) {}
    }

    @FXML
    private void onSave(ActionEvent event) {
        errorLabel.setText("");

        if (disponibiliteBox.getValue() == null) {
            errorLabel.setText("⚠️ Choisissez un créneau !");
            return;
        }
        if (animalNomField.getText().trim().isEmpty()) {
            errorLabel.setText("⚠️ Entrez le nom de votre animal !");
            return;
        }
        if (animalTypeBox.getValue() == null) {
            errorLabel.setText("⚠️ Choisissez le type d'animal !");
            return;
        }
        if (numField.getText().trim().isEmpty()) {
            errorLabel.setText("⚠️ Entrez votre numéro de téléphone !");
            return;
        }
        if (descriptionArea.getText().trim().isEmpty()) {
            errorLabel.setText("⚠️ Ajoutez une description !");
            return;
        }

        try {
            int dispoId  = disponibiliteIds.get(disponibiliteBox.getValue());
            int clientId = SessionManager.getUserId();
            int vetId    = SessionManager.getSelectedVetId();

            final int num = Integer.parseInt(
                    numField.getText().trim().replaceAll("[^0-9]", "")
            );
            String slotStart = slotStartTimes.get(disponibiliteBox.getValue());
            if (serviceRdv.slotAlreadyTaken(vetId, dispoId, slotStart)) {
                errorLabel.setText("⚠️ Ce créneau est déjà réservé !");
                return;
            }

            String animalInfo = animalNomField.getText().trim() +
                    " (" + animalTypeBox.getValue() + ")";

            Rendezvous rdv = new Rendezvous();
            rdv.setClient_id(clientId);
            rdv.setVet_id(vetId);
            rdv.setAnimal_id(0);
            rdv.setDisponibilite_id(dispoId);
            rdv.setSlotStart(slotStart);
            rdv.setStatus("EN_ATTENTE");
            rdv.setDescription("[🐾 " + animalInfo + "] [⏰ " + slotStart + "] " +
                    descriptionArea.getText().trim());
            rdv.setNum(num);

            serviceRdv.add(rdv);


            CompletableFuture.runAsync(() -> {
                try {
                    String vetEmail = serviceRdv.getVetEmail(vetId);
                    String vetNom   = serviceRdv.getVetNom(vetId);
                    emailService.notifyVetNewRdv(
                            vetEmail, vetNom,
                            String.valueOf(num),
                            rdv.getDescription()
                    );
                } catch (Exception ignored) {}
            });

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("✅ Succès");
            alert.setHeaderText(null);
            alert.setContentText("Votre rendez-vous a été envoyé !\nEn attente de confirmation. 😊");
            alert.showAndWait();

            ViewNavigator.goTo(event, "/DashboardClient.fxml", "Mon Espace");

        } catch (Exception e) {
            errorLabel.setText("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onRefreshDisponibilites(ActionEvent event) {
        disponibiliteBox.getItems().clear();
        disponibiliteIds.clear();
        slotStartTimes.clear(); // ✅
        loadDisponibilites();
    }

    @FXML
    private void onGoHome(ActionEvent event) {
        ViewNavigator.goTo(event, "/ListeVeterinaires.fxml", "Nos Vétérinaires");
    }
}