package com.esprit.furhope.controllers;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import com.esprit.furhope.entities.post;
import com.esprit.furhope.services.CommentServiceJdbc;
import com.esprit.furhope.services.FunApiService;
import com.esprit.furhope.services.PostServiceJdbc;
import com.esprit.furhope.ui.PostCard;
import com.esprit.furhope.utils.AppSession;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
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
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class feedcontroller {

    @FXML
    private VBox postsbox;

    @FXML
    private ScrollPane feedScrollPane;

    @FXML
    private Button addPostFab;

    @FXML
    private ScrollPane funScrollPane;

    @FXML
    private VBox funPanelBox;

    @FXML
    private Label jokeContentLabel;

    @FXML
    private Label quoteContentLabel;

    @FXML
    private Label catFactContentLabel;

    @FXML
    private Label memeTitleLabel;

    @FXML
    private Label jokeErrorLabel;

    @FXML
    private Label quoteErrorLabel;

    @FXML
    private Label catFactErrorLabel;

    @FXML
    private Label dogErrorLabel;

    @FXML
    private Label memeErrorLabel;

    @FXML
    private Label dogPlaceholderLabel;

    @FXML
    private Label memePlaceholderLabel;

    @FXML
    private ProgressIndicator jokeLoadingIndicator;

    @FXML
    private ProgressIndicator quoteLoadingIndicator;

    @FXML
    private ProgressIndicator catFactLoadingIndicator;

    @FXML
    private ProgressIndicator dogLoadingIndicator;

    @FXML
    private ProgressIndicator memeLoadingIndicator;

    @FXML
    private ImageView dogImageView;

    @FXML
    private ImageView memeImageView;

    private final PostServiceJdbc postSvc = new PostServiceJdbc();
    private final CommentServiceJdbc commentSvc = new CommentServiceJdbc();
    private final FunApiService funApiService = new FunApiService();
    private List<post> allPosts = new ArrayList<>();
    private String searchQuery = "";

    @FXML
    public void initialize() {
        configureFabIcon();
        configureFunPanel();
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

    private void configureFunPanel() {
        setupRoundedClip(dogImageView, 18);
        setupRoundedClip(memeImageView, 18);

        if (dogImageView != null) {
            dogImageView.setSmooth(true);
        }
        if (memeImageView != null) {
            memeImageView.setSmooth(true);
        }

        loadJoke();
        loadQuote();
        loadCatFact();
        loadDogImage();
        loadMeme();
    }

    @FXML
    private void onNewJokeClick() {
        loadJoke();
    }

    @FXML
    private void onNewQuoteClick() {
        loadQuote();
    }

    @FXML
    private void onNewCatFactClick() {
        loadCatFact();
    }

    @FXML
    private void onNewDogClick() {
        loadDogImage();
    }

    @FXML
    private void onNewMemeClick() {
        loadMeme();
    }

    private void loadJoke() {
        loadTextCard(funApiService.fetchRandomJoke(), jokeContentLabel, jokeLoadingIndicator, jokeErrorLabel);
    }

    private void loadQuote() {
        loadTextCard(funApiService.fetchRandomQuote(), quoteContentLabel, quoteLoadingIndicator, quoteErrorLabel);
    }

    private void loadCatFact() {
        loadTextCard(funApiService.fetchCatFact(), catFactContentLabel, catFactLoadingIndicator, catFactErrorLabel);
    }

    private void loadTextCard(CompletableFuture<String> fetchFuture, Label contentLabel, ProgressIndicator loadingIndicator, Label errorLabel) {
        setLoading(loadingIndicator, true);
        setError(errorLabel, null);

        fetchFuture
                .thenAccept(text -> Platform.runLater(() -> {
                    if (contentLabel != null) {
                        contentLabel.setText(text);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> setError(errorLabel, friendlyMessage(ex)));
                    return null;
                })
                .whenComplete((unused, unusedErr) -> Platform.runLater(() -> setLoading(loadingIndicator, false)));
    }

    private void loadDogImage() {
        setLoading(dogLoadingIndicator, true);
        setError(dogErrorLabel, null);
        setPlaceholder(dogPlaceholderLabel, "Loading dog image...", true);

        funApiService.fetchRandomDogImageUrl()
                .thenAccept(url -> Platform.runLater(() ->
                        loadRemoteImage(url, dogImageView, dogPlaceholderLabel, dogLoadingIndicator, dogErrorLabel, "Tap New Dog to load.")
                ))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setLoading(dogLoadingIndicator, false);
                        setError(dogErrorLabel, friendlyMessage(ex));
                        setPlaceholder(dogPlaceholderLabel, "Tap New Dog to load.", true);
                    });
                    return null;
                });
    }

    private void loadMeme() {
        setLoading(memeLoadingIndicator, true);
        setError(memeErrorLabel, null);
        setPlaceholder(memePlaceholderLabel, "Loading meme image...", true);

        funApiService.fetchRandomMeme()
                .thenAccept(meme -> Platform.runLater(() -> {
                    if (memeTitleLabel != null) {
                        memeTitleLabel.setText(meme.title());
                    }
                    loadRemoteImage(meme.imageUrl(), memeImageView, memePlaceholderLabel, memeLoadingIndicator, memeErrorLabel, "Waiting for meme...");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        setLoading(memeLoadingIndicator, false);
                        setError(memeErrorLabel, friendlyMessage(ex));
                        setPlaceholder(memePlaceholderLabel, "Waiting for meme...", true);
                    });
                    return null;
                });
    }

    private void loadRemoteImage(String imageUrl,
                                 ImageView imageView,
                                 Label placeholderLabel,
                                 ProgressIndicator loadingIndicator,
                                 Label errorLabel,
                                 String idlePlaceholderText) {
        if (imageUrl == null || imageUrl.isBlank() || imageView == null) {
            setLoading(loadingIndicator, false);
            setError(errorLabel, "Service unavailable, try again");
            setPlaceholder(placeholderLabel, idlePlaceholderText, true);
            return;
        }

        setError(errorLabel, null);
        setPlaceholder(placeholderLabel, "Loading image...", true);

        Image image = new Image(imageUrl, true);
        imageView.setImage(image);

        image.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.doubleValue() >= 1.0 && !image.isError()) {
                Platform.runLater(() -> {
                    setPlaceholder(placeholderLabel, "", false);
                    setLoading(loadingIndicator, false);
                });
            }
        });

        image.exceptionProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Platform.runLater(() -> {
                    imageView.setImage(null);
                    setLoading(loadingIndicator, false);
                    setError(errorLabel, "Service unavailable, try again");
                    setPlaceholder(placeholderLabel, idlePlaceholderText, true);
                });
            }
        });

        if (image.getProgress() >= 1.0 && !image.isError()) {
            setPlaceholder(placeholderLabel, "", false);
            setLoading(loadingIndicator, false);
        }
    }

    private void setPlaceholder(Label placeholderLabel, String text, boolean show) {
        if (placeholderLabel == null) return;
        placeholderLabel.setText(text == null ? "" : text);
        placeholderLabel.setVisible(show);
        placeholderLabel.setManaged(show);
    }

    private void setLoading(ProgressIndicator indicator, boolean loading) {
        if (indicator == null) return;
        indicator.setVisible(loading);
        indicator.setManaged(loading);
    }

    private void setError(Label errorLabel, String message) {
        if (errorLabel == null) return;
        boolean show = message != null && !message.isBlank();
        errorLabel.setText(show ? message : "");
        errorLabel.setVisible(show);
        errorLabel.setManaged(show);
    }

    private void setupRoundedClip(ImageView imageView, double radius) {
        if (imageView == null) return;
        Rectangle clip = new Rectangle();
        clip.setArcWidth(radius);
        clip.setArcHeight(radius);
        clip.widthProperty().bind(imageView.fitWidthProperty());
        clip.heightProperty().bind(imageView.fitHeightProperty());
        imageView.setClip(clip);
    }

    private String friendlyMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof java.util.concurrent.CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return "Service unavailable, try again";
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

        ChoiceBox<String> visibilityChoice = new ChoiceBox<>();
        visibilityChoice.getItems().addAll("PUBLIC", "FRIENDS", "PRIVATE");
        visibilityChoice.setValue("PUBLIC");

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
            post p = new post();
            p.setAuthorId(AppSession.getCurrentUserId());
            p.setCaption(caption);
            p.setDurationSeconds(null);
            p.setVisibility(visibilityChoice.getValue());
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

            Task<Void> createTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    postSvc.ajouter(p);
                    return null;
                }
            };
            createTask.setOnSucceeded(evt -> {
                loadPosts();
                if (previewPlayer[0] != null) {
                    previewPlayer[0].dispose();
                    previewPlayer[0] = null;
                }
                dialogStage.close();
            });
            createTask.setOnFailed(evt -> {
                Throwable ex = createTask.getException();
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Failed to save post: " + ex.getMessage()).showAndWait();
            });
            Thread thread = new Thread(createTask, "post-create-jdbc");
            thread.setDaemon(true);
            thread.start();
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
        Label visibilitySectionLabel = new Label("Visibility");
        visibilitySectionLabel.getStyleClass().add("composer-section-title");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("composer-card");
        root.getChildren().addAll(
                headerBox,
                captionLabel,
                captionArea,
                visibilitySectionLabel,
                visibilityChoice,
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

    public void reloadFeed() {
        loadPosts();
    }

    public void focusPost(long postId) {
        if (searchQuery != null && !searchQuery.isBlank()) {
            searchQuery = "";
            renderPosts();
        }
        loadPosts(() -> scrollToPost(postId));
    }

    private void loadPosts() {
        loadPosts(null);
    }

    private void loadPosts(Runnable onSuccess) {
        Task<List<post>> task = new Task<>() {
            @Override
            protected List<post> call() throws Exception {
                return postSvc.afficher(AppSession.getCurrentUserId());
            }
        };

        task.setOnSucceeded(event -> {
            allPosts = task.getValue();
            renderPosts();
            if (onSuccess != null) onSuccess.run();
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to load feed from database: " + ex.getMessage()).showAndWait();
        });

        Thread thread = new Thread(task, "feed-jdbc-load");
        thread.setDaemon(true);
        thread.start();
    }

    private void scrollToPost(long postId) {
        if (postsbox == null || feedScrollPane == null) return;

        postsbox.applyCss();
        postsbox.layout();

        for (Node node : postsbox.getChildren()) {
            if (!(node instanceof PostCard card)) continue;
            if (card.getPostId() != postId) continue;

            Bounds viewport = feedScrollPane.getViewportBounds();
            double contentHeight = postsbox.getBoundsInLocal().getHeight();
            double nodeTop = node.getBoundsInParent().getMinY();
            double maxScroll = Math.max(1, contentHeight - viewport.getHeight());
            double target = Math.min(Math.max(nodeTop / maxScroll, 0), 1);

            double start = feedScrollPane.getVvalue();
            Timeline smoothScroll = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(feedScrollPane.vvalueProperty(), start, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.millis(420), new KeyValue(feedScrollPane.vvalueProperty(), target, Interpolator.EASE_BOTH))
            );
            smoothScroll.setOnFinished(event -> card.playFocusTransition());
            smoothScroll.play();
            break;
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
        String userName = (p.getAuthorName() == null ? ("user " + p.getAuthorId()) : p.getAuthorName()).toLowerCase(Locale.ROOT);
        String caption = p.getCaption() == null ? "" : p.getCaption().toLowerCase(Locale.ROOT);

        return userName.contains(query) || caption.contains(query);
    }

    @FXML
    private void onBackToHome(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/accueil.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Unable to return to home page.").showAndWait();
        }
    }
}
