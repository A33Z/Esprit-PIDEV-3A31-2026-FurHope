package com.esprit.services.ai;

public class ReclamationAutoReplyDecision {

    private final String intent;
    private final boolean requiresHumanReview;
    private final boolean autoSendCandidate;
    private final String draftMessage;
    private final String reason;

    public ReclamationAutoReplyDecision(
            String intent,
            boolean requiresHumanReview,
            boolean autoSendCandidate,
            String draftMessage,
            String reason
    ) {
        this.intent = intent;
        this.requiresHumanReview = requiresHumanReview;
        this.autoSendCandidate = autoSendCandidate;
        this.draftMessage = draftMessage;
        this.reason = reason;
    }

    public String getIntent() {
        return intent;
    }

    public boolean isRequiresHumanReview() {
        return requiresHumanReview;
    }

    public boolean isAutoSendCandidate() {
        return autoSendCandidate;
    }

    public String getDraftMessage() {
        return draftMessage;
    }

    public String getReason() {
        return reason;
    }
}
