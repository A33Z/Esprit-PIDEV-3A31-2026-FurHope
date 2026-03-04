package com.esprit.services.ai;

import com.esprit.entities.Reclamation;

import java.util.Locale;

public class RuleBasedSmartReplyGenerator implements SmartReplyGenerator {

    @Override
    public SmartReplyGeneration generate(Reclamation reclamation) {
        String text = normalize(reclamation == null ? null : (reclamation.getSujet() + " " + reclamation.getDescription()));
        if (text.isEmpty()) {
            return requiresReview("UNKNOWN", "Missing text to classify.");
        }

        if (containsAny(text, "lawyer", "attorney", "legal", "court", "fraud", "police", "sue", "threat", "urgent")) {
            return requiresReview("HIGH_RISK", "Message contains high-risk or legal/escalation language.");
        }

        if (containsAny(text, "status", "update", "where is", "when", "progress")) {
            return new SmartReplyGeneration(
                    "Thank you for your message. Your reclamation is registered and currently being reviewed. " +
                            "You will receive a status update as soon as the review is completed.",
                    "STATUS_UPDATE",
                    0.88,
                    false,
                    "Low-risk status request."
            );
        }

        if (containsAny(text, "document", "invoice", "receipt", "proof", "attachment", "piece jointe")) {
            return new SmartReplyGeneration(
                    "Thank you for your message. Please share any supporting documents in reply to this thread " +
                            "so we can process your reclamation faster.",
                    "DOCUMENT_REQUEST",
                    0.9,
                    false,
                    "Low-risk document request."
            );
        }

        if (containsAny(text, "delay", "late", "not received", "didn't receive", "pas recu", "retard")) {
            return new SmartReplyGeneration(
                    "Thank you for reporting this delay. We have started checking your case and will follow up " +
                            "with a concrete update shortly.",
                    "DELIVERY_DELAY",
                    0.84,
                    false,
                    "Low-risk delay complaint."
            );
        }

        return requiresReview("OTHER", "Message intent is ambiguous.");
    }

    private SmartReplyGeneration requiresReview(String intent, String reason) {
        return new SmartReplyGeneration(
                "Thank you for your message. Your reclamation was received and has been escalated to our support team. " +
                        "An admin will reply shortly.",
                intent,
                0.6,
                true,
                reason
        );
    }

    private boolean containsAny(String source, String... terms) {
        for (String term : terms) {
            if (source.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replace('\n', ' ').trim();
    }
}
