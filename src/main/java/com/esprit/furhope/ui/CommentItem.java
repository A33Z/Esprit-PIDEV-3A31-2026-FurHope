package com.esprit.furhope.ui;

import com.esprit.furhope.entities.comment;
import com.esprit.furhope.utils.TimeUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Single comment or reply row. Supports nested replies via indent and style.
 */
public class CommentItem extends VBox {

    private static final int REPLY_INDENT = 24;

    private final comment c;
    private final boolean isReply;
    private final VBox repliesContainer;
    private final Label likeCountLabel;
    private final Label dislikeCountLabel;

    public CommentItem(comment c, List<comment> replies, boolean isReply,
                       java.util.function.Consumer<comment> onReplyClicked,
                       java.util.function.Consumer<comment> onDeleteClicked) {
        this.c = c;
        this.isReply = isReply;
        getStyleClass().add(isReply ? "reply-item" : "comment-item");

        setSpacing(6);
        setPadding(new Insets(isReply ? 10 : 12, isReply ? REPLY_INDENT : 16, 12, isReply ? REPLY_INDENT : 16));
        if (isReply) {
            setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 8;");
        }

        Region avatar = new Region();
        avatar.getStyleClass().add("comment-avatar");
        avatar.setMinSize(28, 28);
        avatar.setPrefSize(28, 28);
        avatar.setMaxSize(28, 28);

        Label authorLabel = new Label("User " + c.getAuthorId());
        authorLabel.getStyleClass().add("comment-author");
        Label timeLabel = new Label(TimeUtils.formatAgo(c.getCreatedAt()));
        timeLabel.getStyleClass().add("comment-time");

        HBox metaRow = new HBox(8, authorLabel, timeLabel);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label bodyLabel = new Label(c.getBody() != null ? c.getBody() : "");
        bodyLabel.getStyleClass().add("comment-body");
        bodyLabel.setWrapText(true);

        int likes = 0;
        int dislikes = 0;
        likeCountLabel = new Label(String.valueOf(likes));
        likeCountLabel.getStyleClass().add("comment-count");
        dislikeCountLabel = new Label(String.valueOf(dislikes));
        dislikeCountLabel.getStyleClass().add("comment-count");

        Button likeBtn = new Button("Like");
        likeBtn.getStyleClass().add("action-button-small");
        likeBtn.setOnAction(e -> { /* TODO: increment comment like */ });
        Button dislikeBtn = new Button("Dislike");
        dislikeBtn.getStyleClass().add("action-button-small");
        dislikeBtn.setOnAction(e -> { /* TODO: increment comment dislike */ });
        Button replyBtn = new Button("Reply");
        replyBtn.getStyleClass().add("action-button-small");
        replyBtn.setOnAction(e -> {
            if (onReplyClicked != null) onReplyClicked.accept(c);
        });
        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("action-button-small");
        deleteBtn.setStyle("-fx-text-fill: #b02828;");
        deleteBtn.setOnAction(e -> {
            if (onDeleteClicked != null) onDeleteClicked.accept(c);
        });

        HBox actionsRow = new HBox(12, likeBtn, likeCountLabel, dislikeBtn, dislikeCountLabel, replyBtn, deleteBtn);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(metaRow, bodyLabel, actionsRow);

        repliesContainer = new VBox(6);
        repliesContainer.setPadding(new Insets(8, 0, 0, REPLY_INDENT));
        if (replies != null && !replies.isEmpty()) {
            for (comment reply : replies) {
                repliesContainer.getChildren().add(new CommentItem(reply, null, true, onReplyClicked, onDeleteClicked));
            }
            getChildren().add(repliesContainer);
        }
    }

    public comment getComment() { return c; }
}
