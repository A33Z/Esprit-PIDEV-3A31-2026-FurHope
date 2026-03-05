package com.esprit.services.ai;

public class ReclamationAutoReplyResult {

    private final boolean enabled;
    private final boolean autoSent;
    private final String draftMessage;
    private final String summary;

    public ReclamationAutoReplyResult(boolean enabled, boolean autoSent, String draftMessage, String summary) {
        this.enabled = enabled;
        this.autoSent = autoSent;
        this.draftMessage = draftMessage;
        this.summary = summary;
    }

    public static ReclamationAutoReplyResult disabled(String summary) {
        return new ReclamationAutoReplyResult(false, false, "", summary);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAutoSent() {
        return autoSent;
    }

    public String getDraftMessage() {
        return draftMessage;
    }

    public String getSummary() {
        return summary;
    }

    public boolean hasDraftMessage() {
        return draftMessage != null && !draftMessage.trim().isEmpty();
    }
}
