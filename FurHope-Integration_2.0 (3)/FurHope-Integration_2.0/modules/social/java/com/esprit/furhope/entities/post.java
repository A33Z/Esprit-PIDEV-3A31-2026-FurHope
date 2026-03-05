package com.esprit.furhope.entities;

import java.sql.Timestamp;
import java.util.Objects;

public class post {

    private long id;
    private int authorId;
    private String authorName;
    private String authorProfileImagePath;
    private String caption;

    private String mediaType;
    private String mediaPath;
    private String thumbnailPath;
    private Integer durationSeconds;

    private int likesCount;
    private int dislikesCount;
    private int sharesCount;
    private int commentsCount;

    private String visibility;
    private String status;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    public post() {}

    public post(int authorId, String caption, String mediaType, String mediaPath,
                String thumbnailPath, Integer durationSeconds, String visibility, String status) {
        this.authorId = authorId;
        this.caption = caption;
        this.mediaType = mediaType;
        this.mediaPath = mediaPath;
        this.thumbnailPath = thumbnailPath;
        this.durationSeconds = durationSeconds;
        this.visibility = visibility;
        this.status = status;
    }

    public post(long id, int authorId, String caption, String mediaType, String mediaPath,
                String thumbnailPath, Integer durationSeconds, int likesCount, int dislikesCount,
                int sharesCount, int commentsCount, String visibility, String status,
                Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.authorId = authorId;
        this.caption = caption;
        this.mediaType = mediaType;
        this.mediaPath = mediaPath;
        this.thumbnailPath = thumbnailPath;
        this.durationSeconds = durationSeconds;
        this.likesCount = likesCount;
        this.dislikesCount = dislikesCount;
        this.sharesCount = sharesCount;
        this.commentsCount = commentsCount;
        this.visibility = visibility;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorProfileImagePath() { return authorProfileImagePath; }
    public void setAuthorProfileImagePath(String authorProfileImagePath) { this.authorProfileImagePath = authorProfileImagePath; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public String getMediaPath() { return mediaPath; }
    public void setMediaPath(String mediaPath) { this.mediaPath = mediaPath; }

    public String getThumbnailPath() { return thumbnailPath; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

    public int getDislikesCount() { return dislikesCount; }
    public void setDislikesCount(int dislikesCount) { this.dislikesCount = dislikesCount; }

    public int getSharesCount() { return sharesCount; }
    public void setSharesCount(int sharesCount) { this.sharesCount = sharesCount; }

    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "post{" +
                "id=" + id +
                ", authorId=" + authorId +
                ", caption='" + caption + '\'' +
                ", mediaType='" + mediaType + '\'' +
                ", visibility='" + visibility + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof post)) return false;
        post post = (post) o;
        return id == post.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
