package com.esprit.furhope.integration.api.dto;

import java.sql.Timestamp;

public class NotificationDto {
    private long id;
    private int actorId;
    private String actorName;
    private String type;
    private String message;
    private boolean isRead;
    private Timestamp createdAt;
    private Long postId;
    private Long commentId;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public int getActorId() { return actorId; }
    public void setActorId(int actorId) { this.actorId = actorId; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }
}
