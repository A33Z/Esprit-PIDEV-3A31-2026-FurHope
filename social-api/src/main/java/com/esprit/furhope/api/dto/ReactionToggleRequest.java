package com.esprit.furhope.api.dto;

public class ReactionToggleRequest {
    private long userId;
    private String reaction;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getReaction() {
        return reaction;
    }

    public void setReaction(String reaction) {
        this.reaction = reaction;
    }
}
