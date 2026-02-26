package com.esprit.furhope.controllers;

import com.esprit.furhope.entities.post;
import com.esprit.furhope.services.commentService;
import com.esprit.furhope.services.postService;
import com.esprit.furhope.ui.PostCard;
import com.esprit.furhope.utils.AppSession;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class feedcontroller {

    @FXML
    private VBox postsbox;

    @FXML
    private Button addPostFab;

    private final postService postSvc = new postService();
    private final commentService commentSvc = new commentService();
    private List<post> allPosts = new ArrayList<>();
    private String searchQuery = "";

    @FXML
    public void initialize() {
        configureFabIcon();
        loadPosts();
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        renderPosts();
    }

    private void configureFabIcon() {
        if (addPostFab == null) return;

        SVGPath plus = new SVGPath();
        plus.setContent("M11 4H13V11H20V13H13V20H11V13H4V11H11Z");
        plus.getStyleClass().add("fab-icon-shape");

        StackPane iconWrap = new StackPane(plus);
        iconWrap.getStyleClass().add("fab-icon-wrap");
        iconWrap.setMinSize(22, 22);
        iconWrap.setPrefSize(22, 22);
        iconWrap.setMaxSize(22, 22);

        addPostFab.setGraphic(iconWrap);
        addPostFab.setText("");
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
        captionArea.getStyleClass().add("composer-caption");

        Label mediaLabel = new Label("No media chosen");
        mediaLabel.getStyleClass().add("composer-meta");

        ImageView imagePreview = new ImageView();
        imagePreview.setFitWidth(300);
        imagePreview.setFitHeight(220);
        imagePreview.setPreserveRatio(true);
        imagePreview.setVisible(false);
        imagePreview.setManaged(false);

        MediaView videoPreview = new MediaView();
        videoPreview.setFitWidth(300);
        videoPreview.setFitHeight(220);
        videoPreview.setPreserveRatio(true);
        videoPreview.setVisible(false);
        videoPreview.setManaged(false);

        Label mediaTypeLabel = new Label("");
        mediaTypeLabel.getStyleClass().add("composer-meta-secondary");

        File[] chosenFile = new File[1];
        MediaPlayer[] previewPlayer = new MediaPlayer[1];

        Button chooseMediaBtn = new Button("Choose media");
        chooseMediaBtn.getStyleClass().add("composer-btn-secondary");
        chooseMediaBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select image or video");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Media", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.mp4", "*.mov", "*.m4v"),
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                    new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.mov", "*.m4v")
            );
            File f = fc.showOpenDialog(dialogStage);
            if (f == null) return;

            chosenFile[0] = f;
            mediaLabel.setText(f.getName());
            imagePreview.setVisible(false);
            imagePreview.setManaged(false);
            imagePreview.setImage(null);
            videoPreview.setVisible(false);
            videoPreview.setManaged(false);
            videoPreview.setMediaPlayer(null);
            if (previewPlayer[0] != null) {
                previewPlayer[0].dispose();
                previewPlayer[0] = null;
            }

            try {
                if (isVideoFile(f)) {
                    mediaTypeLabel.setText("Video selected");
                    Media media = new Media(f.toURI().toString());
                    MediaPlayer player = new MediaPlayer(media);
                    player.setAutoPlay(true);
                    player.setMute(true);
                    player.setCycleCount(1);
                    player.setOnError(() -> mediaTypeLabel.setText("Video codec not supported. Try MP4 (H.264/AAC)."));
                    previewPlayer[0] = player;
                    videoPreview.setMediaPlayer(player);
                    videoPreview.setVisible(true);
                    videoPreview.setManaged(true);
                } else {
                    mediaTypeLabel.setText("Image selected");
                    Image img = new Image(f.toURI().toString(), false);
                    if (img.isError()) {
                        throw new IllegalArgumentException("Unsupported image format");
                    }
                    imagePreview.setImage(img);
                    imagePreview.setVisible(true);
                    imagePreview.setManaged(true);
                }
            } catch (Exception ex) {
                mediaTypeLabel.setText("Preview unavailable. Use JPG/PNG/GIF/BMP or MP4/M4V/MOV.");
            }
        });

        Button clearMediaBtn = new Button("Remove media");
        clearMediaBtn.getStyleClass().add("composer-btn-secondary");
        clearMediaBtn.setOnAction(e -> {
            chosenFile[0] = null;
            mediaLabel.setText("No media chosen");
            mediaTypeLabel.setText("");
            imagePreview.setImage(null);
            imagePreview.setVisible(false);
            imagePreview.setManaged(false);
            videoPreview.setMediaPlayer(null);
            videoPreview.setVisible(false);
            videoPreview.setManaged(false);
            if (previewPlayer[0] != null) {
                previewPlayer[0].dispose();
                previewPlayer[0] = null;
            }
        });

        HBox mediaBox = new HBox(10, chooseMediaBtn, clearMediaBtn);
        mediaBox.setAlignment(Pos.CENTER_LEFT);
        mediaBox.getStyleClass().add("composer-media-actions");

        VBox previewBox = new VBox(6, mediaLabel, mediaTypeLabel, imagePreview, videoPreview);
        previewBox.setAlignment(Pos.CENTER_LEFT);
        previewBox.getStyleClass().add("composer-preview-box");

        Button postBtn = new Button("Post");
        Button cancelBtn = new Button("Cancel");
        postBtn.getStyleClass().add("composer-btn-primary");
        cancelBtn.getStyleClass().add("composer-btn-secondary");
        postBtn.setDefaultButton(true);

        postBtn.setOnAction(e -> {
            String caption = captionArea.getText().trim();
            if (caption.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please enter a caption.").showAndWait();
                return;
            }
            try {
                post p = new post();
                p.setAuthorId(AppSession.getCurrentUserId());
                p.setCaption(caption);
                p.setDurationSeconds(null);
                p.setVisibility("PUBLIC");
                p.setStatus("ACTIVE");
                if (chosenFile[0] != null) {
                    p.setMediaType(isVideoFile(chosenFile[0]) ? "VIDEO" : "IMAGE");
                    p.setMediaPath(chosenFile[0].getAbsolutePath());
                    p.setThumbnailPath(null);
                } else {
                    p.setMediaType("NONE");
                    p.setMediaPath(null);
                    p.setThumbnailPath(null);
                }
                postSvc.ajouter(p);
                loadPosts();
                if (previewPlayer[0] != null) {
                    previewPlayer[0].dispose();
                    previewPlayer[0] = null;
                }
                dialogStage.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Failed to save post: " + ex.getMessage()).showAndWait();
            }
        });
        cancelBtn.setOnAction(e -> {
            if (previewPlayer[0] != null) {
                previewPlayer[0].dispose();
                previewPlayer[0] = null;
            }
            dialogStage.close();
        });

        HBox buttons = new HBox(10, postBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.getStyleClass().add("composer-footer");

        Label title = new Label("Create Post");
        title.getStyleClass().add("composer-title");
        Label subtitle = new Label("Share photos, clips, and updates with your community.");
        subtitle.getStyleClass().add("composer-subtitle");
        VBox headerBox = new VBox(2, title, subtitle);
        headerBox.getStyleClass().add("composer-header");

        Label captionLabel = new Label("Caption");
        captionLabel.getStyleClass().add("composer-section-title");

        Label mediaSectionLabel = new Label("Media (optional)");
        mediaSectionLabel.getStyleClass().add("composer-section-title");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("composer-card");
        root.getChildren().addAll(
                headerBox,
                captionLabel,
                captionArea,
                mediaSectionLabel,
                mediaBox,
                previewBox,
                new Separator(),
                buttons
        );
        VBox.setVgrow(captionArea, Priority.NEVER);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("composer-scroll");

        StackPane shell = new StackPane(scroll);
        shell.getStyleClass().add("composer-shell");

        Scene scene = new Scene(shell, 560, 640);
        if (getClass().getResource("/css/app.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        }
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private boolean isVideoFile(File file) {
        if (file == null) return false;
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".mp4") || name.endsWith(".mov") || name.endsWith(".m4v");
    }

    private void loadPosts() {
        try {
            allPosts = postSvc.afficher();
            renderPosts();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void renderPosts() {
        if (postsbox == null) return;

        postsbox.getChildren().clear();
        for (post p : allPosts) {
            if (!matchesSearch(p)) continue;
            PostCard card = new PostCard(p, postSvc, commentSvc, this::loadPosts, AppSession.getCurrentUserId());
            postsbox.getChildren().add(card);
        }

        if (postsbox.getChildren().isEmpty()) {
            Label empty = new Label(searchQuery == null || searchQuery.isBlank()
                    ? "No posts yet."
                    : "No users or posts match your search.");
            empty.getStyleClass().add("placeholder-subtitle");
            postsbox.getChildren().add(empty);
        }
    }

    private boolean matchesSearch(post p) {
        if (searchQuery == null || searchQuery.isBlank()) return true;

        String query = searchQuery.toLowerCase(Locale.ROOT);
        String userName = ("user " + p.getAuthorId()).toLowerCase(Locale.ROOT);
        String caption = p.getCaption() == null ? "" : p.getCaption().toLowerCase(Locale.ROOT);

        return userName.contains(query) || caption.contains(query);
    }
}
