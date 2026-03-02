package com.esprit.furhope.integration.api.dto;

public class ReactionDto {
    private long postId;
    private long userId;
    private String reaction;

    public long getPostId() { return postId; }
    public void setPostId(long postId) { this.postId = postId; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public String getReaction() { return reaction; }
    public void setReaction(String reaction) { this.reaction = reaction; }
}
