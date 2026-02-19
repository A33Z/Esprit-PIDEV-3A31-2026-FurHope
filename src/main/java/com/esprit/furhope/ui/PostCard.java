package com.esprit.furhope.ui;

import com.esprit.furhope.entities.comment;
import com.esprit.furhope.entities.post;
import com.esprit.furhope.services.commentService;
import com.esprit.furhope.services.postService;
import com.esprit.furhope.utils.TimeUtils;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Single post card: header (avatar, user, time), body (caption + media), footer (like/dislike/comment/share), expandable comments.
 */
public class PostCard extends VBox {

    private static final double COMMENT_SECTION_MAX_HEIGHT = 400;
    private static final int FEED_IMAGE_WIDTH = 520;
    private static final int FEED_IMAGE_HEIGHT = 400;

    private final post p;
    private final postService postSvc;
    private final commentService commentSvc;
    private final VBox commentsSection;
    private final VBox commentsList;
    private final Label likeCountLabel;
    private final Label dislikeCountLabel;
    private final Label commentCountLabel;
    private final Label shareCountLabel;
    private comment replyToComment;
    private TextField commentInputRef;

    public PostCard(post p, postService postSvc, commentService commentSvc, Runnable onRefreshFeed) {
        this.p = p;
        this.postSvc = postSvc;
        this.commentSvc = commentSvc;
        getStyleClass().add("post-card");
        setSpacing(0);

        Region avatar = new Region();
        avatar.getStyleClass().add("post-avatar");
        avatar.setMinSize(40, 40);
        avatar.setPrefSize(40, 40);
        avatar.setMaxSize(40, 40);

        Label authorLabel = new Label("User " + p.getAuthorId());
        authorLabel.getStyleClass().add("post-author");
        Label timeLabel = new Label(TimeUtils.formatAgo(p.getCreatedAt()));
        timeLabel.getStyleClass().add("post-meta");

        VBox headerRight = new VBox(2, authorLabel, timeLabel);
        headerRight.setAlignment(Pos.CENTER_LEFT);
        HBox header = new HBox(12, avatar, headerRight);
        header.getStyleClass().add("post-card-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 18, 14, 18));
        getChildren().add(header);

        VBox body = new VBox(10);
        body.getStyleClass().add("post-card-body");
        body.setPadding(new Insets(0, 18, 12, 18));

        if (p.getMediaType() != null && "IMAGE".equalsIgnoreCase(p.getMediaType())
                && p.getMediaPath() != null && !p.getMediaPath().isEmpty()) {
            try {
                String path = p.getMediaPath();
                if (!path.startsWith("file:")) path = "file:" + path;
                ImageView imgView = new ImageView(new Image(path));
                imgView.setFitWidth(FEED_IMAGE_WIDTH);
                imgView.setFitHeight(FEED_IMAGE_HEIGHT);
                imgView.setPreserveRatio(true);
                imgView.setSmooth(true);
                Rectangle clip = new Rectangle();
                clip.setArcWidth(8);
                clip.setArcHeight(8);
                clip.widthProperty().bind(imgView.fitWidthProperty());
                clip.heightProperty().bind(imgView.fitHeightProperty());
                imgView.setClip(clip);
                StackPane imageWrap = new StackPane(imgView);
                imageWrap.getStyleClass().add("post-image-wrap");
                body.getChildren().add(imageWrap);
            } catch (Exception ignored) { }
        }

        Label captionLabel = new Label(p.getCaption() != null ? p.getCaption() : "");
        captionLabel.getStyleClass().add("post-caption");
        captionLabel.setWrapText(true);
        body.getChildren().add(captionLabel);
        getChildren().add(body);

        likeCountLabel = new Label(String.valueOf(p.getLikesCount()));
        dislikeCountLabel = new Label(String.valueOf(p.getDislikesCount()));
        commentCountLabel = new Label(String.valueOf(p.getCommentsCount()));
        shareCountLabel = new Label(String.valueOf(p.getSharesCount()));
        likeCountLabel.getStyleClass().add("footer-count");
        dislikeCountLabel.getStyleClass().add("footer-count");
        commentCountLabel.getStyleClass().add("footer-count");
        shareCountLabel.getStyleClass().add("footer-count");

        Button likeBtn = actionButton("Like", likeCountLabel);
        Button dislikeBtn = actionButton("Dislike", dislikeCountLabel);
        Button commentBtn = actionButton("Comment", commentCountLabel);
        Button shareBtn = actionButton("Share", shareCountLabel);

        likeBtn.setOnAction(e -> {
            try {
                postSvc.incrementLikes(p.getId(), 1);
                p.setLikesCount(p.getLikesCount() + 1);
                likeCountLabel.setText(String.valueOf(p.getLikesCount()));
            } catch (SQLException ex) { ex.printStackTrace(); }
        });
        dislikeBtn.setOnAction(e -> {
            try {
                postSvc.incrementDislikes(p.getId(), 1);
                p.setDislikesCount(p.getDislikesCount() + 1);
                dislikeCountLabel.setText(String.valueOf(p.getDislikesCount()));
            } catch (SQLException ex) { ex.printStackTrace(); }
        });
        shareBtn.setOnAction(e -> {
            try {
                postSvc.incrementShares(p.getId(), 1);
                p.setSharesCount(p.getSharesCount() + 1);
                shareCountLabel.setText(String.valueOf(p.getSharesCount()));
            } catch (SQLException ex) { ex.printStackTrace(); }
        });
        commentBtn.setOnAction(e -> toggleComments());

        HBox footer = new HBox(20);
        footer.getStyleClass().add("post-card-footer");
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(6, 18, 10, 18));
        footer.getChildren().addAll(likeBtn, likeCountLabel, dislikeBtn, dislikeCountLabel, commentBtn, commentCountLabel, shareBtn, shareCountLabel);
        getChildren().add(footer);

        commentsSection = new VBox(12);
        commentsSection.getStyleClass().add("post-comments-section");
        commentsSection.setPadding(new Insets(0, 18, 16, 18));
        commentsSection.setMaxHeight(0);
        commentsSection.setClip(new Rectangle(1000, 0));
        commentsSection.setManaged(false);
        commentsSection.setVisible(false);

        commentsList = new VBox(8);
        commentsList.getStyleClass().add("comments-list-inner");
        ScrollPane commentsScroll = new ScrollPane(commentsList);
        commentsScroll.setFitToWidth(true);
        commentsScroll.setStyle("-fx-background-color: transparent;");
        commentsScroll.setMinHeight(120);
        commentsScroll.setPrefHeight(220);

        TextField commentInput = new TextField();
        commentInput.getStyleClass().add("comment-input-field");
        commentInput.setPromptText("Write a comment...");
        commentInput.setPrefHeight(36);
        HBox.setHgrow(commentInput, Priority.ALWAYS);
        Button postCommentBtn = new Button("Post");
        postCommentBtn.getStyleClass().add("btn-comment-submit");
        postCommentBtn.setOnAction(e -> {
            String bodyText = commentInput.getText().trim();
            if (bodyText.isEmpty()) return;
            try {
                comment c = new comment();
                c.setPostId(p.getId());
                c.setAuthorId(1);
                c.setBody(bodyText);
                c.setStatus("ACTIVE");
                if (replyToComment != null) {
                    c.setParentCommentId(replyToComment.getId());
                    replyToComment = null;
                    commentInput.setPromptText("Write a comment...");
                } else {
                    c.setParentCommentId(null);
                }
                commentSvc.ajouter(c);
                commentInput.clear();
                refreshComments();
                p.setCommentsCount(p.getCommentsCount() + 1);
                commentCountLabel.setText(String.valueOf(p.getCommentsCount()));
                if (onRefreshFeed != null) onRefreshFeed.run();
            } catch (SQLException ex) { ex.printStackTrace(); }
        });
        HBox inputRow = new HBox(10, commentInput, postCommentBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.getStyleClass().add("comment-input-bar");
        commentsSection.getChildren().addAll(new Separator(), commentsScroll, inputRow);
        getChildren().add(commentsSection);

        this.commentInputRef = commentInput;
    }

    private Button actionButton(String text, Label countLabel) {
        Button b = new Button(text);
        b.getStyleClass().add("action-button");
        return b;
    }

    private void toggleComments() {
        boolean expand = !commentsSection.isVisible();
        if (expand) {
            refreshComments();
            commentsSection.setManaged(true);
            commentsSection.setVisible(true);
            commentsSection.setMaxHeight(0);
            Timeline t = new Timeline(
                    new KeyFrame(Duration.millis(0), new KeyValue(commentsSection.maxHeightProperty(), 0)),
                    new KeyFrame(Duration.millis(220), new KeyValue(commentsSection.maxHeightProperty(), COMMENT_SECTION_MAX_HEIGHT, Interpolator.EASE_OUT))
            );
            t.play();
        } else {
            commentsSection.setMaxHeight(0);
            commentsSection.setVisible(false);
            commentsSection.setManaged(false);
        }
    }

    private void refreshComments() {
        commentsList.getChildren().clear();
        try {
            List<comment> flat = commentSvc.findByPostId(p.getId());
            List<comment> roots = flat.stream().filter(c -> c.getParentCommentId() == null).collect(Collectors.toList());
            List<comment> withParent = flat.stream().filter(c -> c.getParentCommentId() != null).collect(Collectors.toList());
            for (comment root : roots) {
                List<comment> replies = withParent.stream()
                        .filter(c -> root.getId() == c.getParentCommentId())
                        .collect(Collectors.toList());
                commentsList.getChildren().add(new CommentItem(root, replies, false, this::onReplyClicked, this::onDeleteComment));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void onDeleteComment(comment c) {
        try {
            commentSvc.supprimer(c.getId());
            refreshComments();
            int count = Math.max(0, p.getCommentsCount() - 1);
            p.setCommentsCount(count);
            commentCountLabel.setText(String.valueOf(count));
            if (commentInputRef != null && replyToComment != null && replyToComment.getId() == c.getId()) {
                replyToComment = null;
                commentInputRef.setPromptText("Write a comment...");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void onReplyClicked(comment parent) {
        replyToComment = parent;
        if (commentInputRef != null) {
            commentInputRef.setPromptText("Reply to User " + parent.getAuthorId() + "...");
            commentInputRef.requestFocus();
        }
    }

    public post getPost() { return p; }
}
