package com.esprit.services.ai;

import com.esprit.entities.Reclamation;
import com.esprit.entities.Reponse;
import com.esprit.services.ReponseService;
import com.esprit.services.userservices;

import java.sql.SQLException;
import java.util.OptionalInt;

public class ReclamationAutoReplyCoordinator {

    @FunctionalInterface
    public interface AutoReplyPublisher {
        void publish(int reclamationId, int adminId, String message) throws SQLException;
    }

    private final ReclamationAutoReplyService classifier;
    private final ReclamationAutoReplyConfig config;
    private final AutoReplyPublisher publisher;
    private final userservices userService;

    public ReclamationAutoReplyCoordinator(
            ReclamationAutoReplyService classifier,
            ReclamationAutoReplyConfig config,
            AutoReplyPublisher publisher
    ) {
        this(classifier, config, publisher, new userservices());
    }

    public ReclamationAutoReplyCoordinator(
            ReclamationAutoReplyService classifier,
            ReclamationAutoReplyConfig config,
            AutoReplyPublisher publisher,
            userservices userService
    ) {
        this.classifier = classifier;
        this.config = config;
        this.publisher = publisher;
        this.userService = userService;
    }

    public static ReclamationAutoReplyCoordinator withDefaults() {
        ReponseService reponseService = new ReponseService();
        AutoReplyPublisher publisher = (reclamationId, adminId, message) -> {
            Reponse autoReply = new Reponse();
            autoReply.setReclamationId(reclamationId);
            autoReply.setAdminId(adminId);
            autoReply.setMessage(message);
            reponseService.ajouter(autoReply);
        };
        return new ReclamationAutoReplyCoordinator(
                new ReclamationAutoReplyService(),
                ReclamationAutoReplyConfig.load(),
                publisher
        );
    }

    public ReclamationAutoReplyResult process(Reclamation reclamation) throws SQLException {
        ReclamationAutoReplyDecision decision = classifier.decide(reclamation);
        AutoReplyMode mode = config.getMode();

        if (mode == AutoReplyMode.OFF) {
            return ReclamationAutoReplyResult.disabled("AI auto-replies are disabled.");
        }

        if (mode == AutoReplyMode.DRAFT_ONLY) {
            int adminId = resolveActorAdminId();
            if (adminId <= 0 && reclamation != null && reclamation.getClientId() > 0) {
                adminId = reclamation.getClientId();
            }
            if (adminId > 0) {
                String draftMessage = "[AI DRAFT - REVIEW REQUIRED]\n" + decision.getDraftMessage();
                publisher.publish(reclamation.getId(), adminId, draftMessage);
                return new ReclamationAutoReplyResult(
                        true,
                        false,
                        draftMessage,
                        "Draft generated and saved for admin review (" + decision.getReason() + ")"
                );
            }
            return new ReclamationAutoReplyResult(
                    true,
                    false,
                    decision.getDraftMessage(),
                    "Draft generated (" + decision.getReason() + "). No active admin found to save in Responses."
            );
        }

        if (!decision.isAutoSendCandidate() || decision.isRequiresHumanReview()) {
            return new ReclamationAutoReplyResult(
                    true,
                    false,
                    decision.getDraftMessage(),
                    "Draft generated for admin review (" + decision.getReason() + ")"
            );
        }

        int adminId = resolveActorAdminId();
        if (adminId <= 0) {
            return new ReclamationAutoReplyResult(
                    true,
                    false,
                    decision.getDraftMessage(),
                    "Auto-send skipped: no active admin account found."
            );
        }

        publisher.publish(reclamation.getId(), adminId, decision.getDraftMessage());
        return new ReclamationAutoReplyResult(
                true,
                true,
                decision.getDraftMessage(),
                "Automatic response sent."
        );
    }

    protected int resolveActorAdminId() {
        if (config.getSystemAdminId() > 0) {
            return config.getSystemAdminId();
        }
        try {
            OptionalInt candidate = userService.findFirstActiveAdminId();
            return candidate.orElse(-1);
        } catch (Exception e) {
            return -1;
        }
    }
}
