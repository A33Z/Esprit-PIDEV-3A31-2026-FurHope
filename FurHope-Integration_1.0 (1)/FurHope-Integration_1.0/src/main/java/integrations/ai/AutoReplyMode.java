package com.esprit.services.ai;

public enum AutoReplyMode {
    OFF,
    DRAFT_ONLY,
    SAFE_AUTO_SEND;

    public static AutoReplyMode fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DRAFT_ONLY;
        }
        String normalized = value.trim().toUpperCase();
        for (AutoReplyMode mode : values()) {
            if (mode.name().equals(normalized)) {
                return mode;
            }
        }
        return DRAFT_ONLY;
    }
}
