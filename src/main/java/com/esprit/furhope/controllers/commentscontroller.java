package com.esprit.furhope.controllers;

import com.esprit.furhope.entities.comment;
import com.esprit.furhope.entities.post;
import com.esprit.furhope.services.commentService;
import com.esprit.furhope.services.postService;
import com.esprit.furhope.utils.TimeUtils;
import com.esprit.furhope.utils.AppSession;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class commentscontroller {

    @FXML
    private TextField bodyfield;

    @FXML
    private VBox commentsbox;

    private final postService postSvc = new postService();
    private final commentService commentSvc = new commentService();

    @FXML
    public void initialize() {
        loadComments();
    }

    @FXML
    private void onAddClick() {
        String bodyText = bodyfield.getText().trim();
        if (bodyText.isEmpty()) return;

        try {
            Long postId = postSvc.getFirstPostId();
            if (postId == null) {
                new Alert(Alert.AlertType.WARNING, "No posts yet. Create a post first.").showAndWait();
                return;
            }
            comment newComment = new comment();
            newComment.setBody(bodyText);
            newComment.setPostId(postId);
            newComment.setAuthorId(AppSession.getCurrentUserId());
            newComment.setStatus("ACTIVE");
            commentSvc.ajouter(newComment);
            bodyfield.clear();
            loadComments();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Load comments grouped by post so the link post ↔ comments is clear. */
    private void loadComments() {
        try {
            commentsbox.getChildren().clear();
            List<post> posts = postSvc.afficher();
            for (post post : posts) {
                List<comment> comments = commentSvc.findByPostId(post.getId());
                if (comments.isEmpty()) continue;

                Label postHeader = new Label("Post: \"" + (post.getCaption() != null ? post.getCaption() : ("#" + post.getId())) + "\"");
                postHeader.getStyleClass().add("section-title");
                postHeader.setStyle("-fx-font-size: 14px; -fx-padding: 12 0 6 0;");
                VBox postSection = new VBox(8);
                postSection.setPadding(new Insets(0, 0, 20, 0));
                postSection.getChildren().add(postHeader);

                for (comment c : comments) {
                    VBox card = createCommentCard(c);
                    postSection.getChildren().add(card);
                }
                commentsbox.getChildren().add(postSection);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox createCommentCard(comment c) {
        VBox card = new VBox(6);
        card.getStyleClass().add("comment-card");
        card.setPadding(new Insets(12, 16, 12, 16));

        Label authorLabel = new Label("User " + c.getAuthorId());
        authorLabel.getStyleClass().add("comment-author");
        Label timeLabel = new Label(TimeUtils.formatAgo(c.getCreatedAt()));
        timeLabel.getStyleClass().add("comment-time");
        HBox meta = new HBox(8, authorLabel, timeLabel);
        meta.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label bodyLabel = new Label(c.getBody() != null ? c.getBody() : "");
        bodyLabel.getStyleClass().add("comment-body");
        bodyLabel.setWrapText(true);

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("action-button-small");
        deleteBtn.setStyle("-fx-text-fill: #b02828;");
        deleteBtn.setOnAction(e -> {
            try {
                commentSvc.supprimer(c.getId());
                loadComments();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("action-button-small");
        editBtn.setOnAction(e -> onEditComment(c));

        HBox actions = new HBox(12, editBtn, deleteBtn);
        actions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.getChildren().addAll(meta, bodyLabel, actions);
        return card;
    }

    private void onEditComment(comment c) {
        TextInputDialog dialog = new TextInputDialog(c.getBody() != null ? c.getBody() : "");
        dialog.setTitle("Edit Comment");
        dialog.setHeaderText("Update comment text");
        dialog.setContentText("Comment:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        String updatedBody = result.get().trim();
        if (updatedBody.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Comment cannot be empty.").showAndWait();
            return;
        }

        try {
            c.setBody(updatedBody);
            if (c.getStatus() == null) c.setStatus("ACTIVE");
            commentSvc.modifier(c);
            loadComments();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to update comment: " + e.getMessage()).showAndWait();
        }
    }
}
