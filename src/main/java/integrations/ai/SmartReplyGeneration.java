package com.esprit.services.ai;

public class SmartReplyGeneration {

    private final String message;
    private final String intent;
    private final double confidence;
    private final boolean requiresHumanReview;
    private final String reason;

    public SmartReplyGeneration(
            String message,
            String intent,
            double confidence,
            boolean requiresHumanReview,
            String reason
    ) {
        this.message = message;
        this.intent = intent;
        this.confidence = confidence;
        this.requiresHumanReview = requiresHumanReview;
        this.reason = reason;
    }

    public String getMessage() {
        return message;
    }

    public String getIntent() {
        return intent;
    }

    public double getConfidence() {
        return confidence;
    }

    public boolean isRequiresHumanReview() {
        return requiresHumanReview;
    }

    public String getReason() {
        return reason;
    }
}
