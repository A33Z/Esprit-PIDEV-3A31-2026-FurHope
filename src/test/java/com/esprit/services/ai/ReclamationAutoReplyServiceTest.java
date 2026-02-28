package com.esprit.services.ai;

import com.esprit.entities.Reclamation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReclamationAutoReplyServiceTest {

    private final ReclamationAutoReplyService service = new ReclamationAutoReplyService();

    @Test
    void shouldMarkStatusRequestsAsLowRiskAutoSendCandidates() {
        Reclamation reclamation = new Reclamation();
        reclamation.setSujet("Status update");
        reclamation.setDescription("Can you tell me where the review is now?");

        ReclamationAutoReplyDecision decision = service.decide(reclamation);

        assertFalse(decision.isRequiresHumanReview());
        assertTrue(decision.isAutoSendCandidate());
        assertTrue(decision.getDraftMessage().toLowerCase().contains("status"));
    }

    @Test
    void shouldEscalateLegalThreatMessagesToHumanReview() {
        Reclamation reclamation = new Reclamation();
        reclamation.setSujet("Legal complaint");
        reclamation.setDescription("I will contact my attorney if this is not resolved.");

        ReclamationAutoReplyDecision decision = service.decide(reclamation);

        assertTrue(decision.isRequiresHumanReview());
        assertFalse(decision.isAutoSendCandidate());
    }

    @Test
    void shouldEscalateAmbiguousMessagesToHumanReview() {
        Reclamation reclamation = new Reclamation();
        reclamation.setSujet("Question");
        reclamation.setDescription("Please call me.");

        ReclamationAutoReplyDecision decision = service.decide(reclamation);

        assertTrue(decision.isRequiresHumanReview());
        assertFalse(decision.isAutoSendCandidate());
    }
}
