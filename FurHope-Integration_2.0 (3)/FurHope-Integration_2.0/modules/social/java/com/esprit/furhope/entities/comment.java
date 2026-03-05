package com.esprit.furhope.entities;

import java.sql.Timestamp;
import java.util.Objects;

public class comment {

    private long id;
    private long postId;
    private int authorId;
    private String authorName;
    private String authorProfileImagePath;

    private Long parentCommentId;
    private String body;

    private String status;
    private Timestamp createdAt;

    public comment() {}

    public comment(long postId, int authorId, Long parentCommentId, String body, String status) {
        this.postId = postId;
        this.authorId = authorId;
        this.parentCommentId = parentCommentId;
        this.body = body;
        this.status = status;
    }

    public comment(long id, long postId, int authorId, Long parentCommentId,
                   String body, String status, Timestamp createdAt) {
        this.id = id;
        this.postId = postId;
        this.authorId = authorId;
        this.parentCommentId = parentCommentId;
        this.body = body;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPostId() { return postId; }
    public void setPostId(long postId) { this.postId = postId; }

    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorProfileImagePath() { return authorProfileImagePath; }
    public void setAuthorProfileImagePath(String authorProfileImagePath) { this.authorProfileImagePath = authorProfileImagePath; }

    public Long getParentCommentId() { return parentCommentId; }
    public void setParentCommentId(Long parentCommentId) { this.parentCommentId = parentCommentId; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "comment{" +
                "id=" + id +
                ", postId=" + postId +
                ", authorId=" + authorId +
                ", authorName='" + authorName + '\'' +
                ", parentCommentId=" + parentCommentId +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", body='" + body + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof comment)) return false;
        comment comment = (comment) o;
        return id == comment.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
