package com.esprit.furhope.controllers;

import com.esprit.furhope.entities.post;
import com.esprit.furhope.services.commentService;
import com.esprit.furhope.services.postService;
import com.esprit.furhope.ui.PostCard;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

public class feedcontroller {

    @FXML
    private VBox postsbox;

    @FXML
    public void initialize() {
        loadPosts();
    }

    @FXML
    private void onNewPostClick() {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("New Post");

        TextArea captionArea = new TextArea();
        captionArea.setPromptText("What's on your mind?");
        captionArea.setWrapText(true);
        captionArea.setPrefRowCount(4);
        captionArea.setMaxWidth(Double.MAX_VALUE);

        Label photoLabel = new Label("No photo chosen");
        photoLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 12;");
        ImageView preview = new ImageView();
        preview.setFitWidth(200);
        preview.setFitHeight(200);
        preview.setPreserveRatio(true);
        preview.setVisible(false);

        java.io.File[] chosenFile = new java.io.File[1];

        Button choosePhotoBtn = new Button("Choose photo");
        choosePhotoBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select image");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
            );
            File f = fc.showOpenDialog(dialogStage.getOwner());
            if (f != null) {
                chosenFile[0] = f;
                photoLabel.setText(f.getName());
                try {
                    preview.setImage(new Image(f.toURI().toString()));
                    preview.setVisible(true);
                } catch (Exception ex) {
                    photoLabel.setText("Preview not available");
                }
            }
        });

        Button clearPhotoBtn = new Button("Remove photo");
        clearPhotoBtn.setOnAction(e -> {
            chosenFile[0] = null;
            photoLabel.setText("No photo chosen");
            preview.setImage(null);
            preview.setVisible(false);
        });

        HBox photoBox = new HBox(10, choosePhotoBtn, clearPhotoBtn);
        photoBox.setAlignment(Pos.CENTER_LEFT);

        VBox previewBox = new VBox(5, photoLabel, preview);
        previewBox.setAlignment(Pos.CENTER_LEFT);

        Button postBtn = new Button("Post");
        Button cancelBtn = new Button("Cancel");
        postBtn.setDefaultButton(true);

        postBtn.setOnAction(e -> {
            String caption = captionArea.getText().trim();
            if (caption.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please enter a caption.").showAndWait();
                return;
            }
            try {
                post p = new post();
                p.setAuthorId(1);
                p.setCaption(caption);
                p.setDurationSeconds(null);
                p.setVisibility("PUBLIC");
                p.setStatus("ACTIVE");
                if (chosenFile[0] != null) {
                    p.setMediaType("IMAGE");
                    p.setMediaPath(chosenFile[0].getAbsolutePath());
                    p.setThumbnailPath(null);
                } else {
                    p.setMediaType("NONE");
                    p.setMediaPath(null);
                    p.setThumbnailPath(null);
                }
                new postService().ajouter(p);
                loadPosts();
                dialogStage.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Failed to save post: " + ex.getMessage()).showAndWait();
            }
        });
        cancelBtn.setOnAction(e -> dialogStage.close());

        HBox buttons = new HBox(10, postBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #fafafa;");
        root.getChildren().addAll(
                new Label("Caption"),
                captionArea,
                new Label("Photo (optional)"),
                photoBox,
                previewBox,
                new Separator(),
                buttons
        );

        Scene scene = new Scene(root, 420, 420);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private void loadPosts() {
        try {
            postService postSvc = new postService();
            commentService commentSvc = new commentService();
            List<post> posts = postSvc.afficher();

            postsbox.getChildren().clear();
            for (post p : posts) {
                PostCard card = new PostCard(p, postSvc, commentSvc, this::loadPosts);
                postsbox.getChildren().add(card);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
