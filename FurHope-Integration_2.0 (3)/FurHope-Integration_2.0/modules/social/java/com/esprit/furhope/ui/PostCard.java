package com.esprit.furhope.ui;

import com.esprit.furhope.entities.comment;
import com.esprit.furhope.entities.post;
import com.esprit.furhope.services.CommentServiceJdbc;
import com.esprit.furhope.services.CommentReactionService;
import com.esprit.furhope.services.PostServiceJdbc;
import com.esprit.furhope.services.ReactionService;
import com.esprit.furhope.services.ReportService;
import com.esprit.furhope.services.ShareService;
import com.esprit.furhope.utils.TimeUtils;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PostCard extends VBox {

    private enum Reaction {
        NONE,
        LIKE,
        DISLIKE
    }

    private static final double COMMENT_SECTION_MAX_HEIGHT = 400;
    private static final int FEED_IMAGE_WIDTH = 520;
    private static final int FEED_IMAGE_HEIGHT = 400;

    private final post p;
    private final PostServiceJdbc postSvc;
    private final CommentServiceJdbc commentSvc;
    private final CommentReactionService commentReactionSvc;
    private final ReactionService reactionSvc;
    private final ReportService reportSvc;
    private final ShareService shareSvc;
    private final VBox commentsSection;
    private final VBox commentsList;
    private final Label likeCountLabel;
    private final Label dislikeCountLabel;
    private final Label commentCountLabel;
    private final Label shareCountLabel;
    private final Label captionLabel;
    private final Runnable onRefreshFeed;
    private final int currentUserId;

    private final Button likeBtn;
    private final Button dislikeBtn;
    private final Button shareBtn;

    private comment replyToComment;
    private TextField commentInputRef;
    private EventHandler<MouseEvent> outsideClickHandler;
    private MediaPlayer mediaPlayer;
    private Reaction currentPostReaction = Reaction.NONE;
    private final Map<Long, CommentReactionService.ReactionSnapshot> commentReactionCache = new ConcurrentHashMap<>();

    public PostCard(post p, PostServiceJdbc postSvc, CommentServiceJdbc commentSvc, Runnable onRefreshFeed, int currentUserId) {
        this.p = p;
        this.postSvc = postSvc;
        this.commentSvc = commentSvc;
        this.commentReactionSvc = new CommentReactionService();
        this.reactionSvc = new ReactionService();
        this.reportSvc = new ReportService();
        this.shareSvc = new ShareService();
        this.onRefreshFeed = onRefreshFeed;
        this.currentUserId = currentUserId;
        getStyleClass().add("post-card");
        setSpacing(0);

        String authorDisplayName = (p.getAuthorName() != null)
                ? p.getAuthorName()
                : ("User " + p.getAuthorId());
        Node avatar = AvatarViewFactory.createAvatar(
                p.getAuthorProfileImagePath(),
                authorDisplayName,
                40,
                "post-avatar"
        );

        Label authorLabel = new Label(authorDisplayName);
        authorLabel.getStyleClass().add("post-author");
        Label timeLabel = new Label(TimeUtils.formatAgo(p.getCreatedAt()));
        timeLabel.getStyleClass().add("post-meta");
        SVGPath visibilityIcon = new SVGPath();
        visibilityIcon.setContent(getVisibilityIconPath(p.getVisibility()));
        visibilityIcon.fillProperty().bind(timeLabel.textFillProperty());
        StackPane visibilityIconWrap = new StackPane(visibilityIcon);
        visibilityIconWrap.setMinSize(15, 15);
        visibilityIconWrap.setPrefSize(15, 15);
        visibilityIconWrap.setMaxSize(15, 15);
        HBox metaRow = new HBox(6, timeLabel, visibilityIconWrap);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Button editPostBtn = smallIconButton("Edit", "M4 17.5V20H6.5L16.8 9.7L14.3 7.2L4 17.5ZM18.7 7.8C19.1 7.4 19.1 6.8 18.7 6.4L17.6 5.3C17.2 4.9 16.6 4.9 16.2 5.3L15.1 6.4L17.6 8.9L18.7 7.8Z", false);
        editPostBtn.setOnAction(e -> onEditPost());

        Button deletePostBtn = smallIconButton("Delete", "M6 7H18V9H17L16 20H8L7 9H6V7ZM9 5H15V6H9V5Z", true);
        deletePostBtn.setOnAction(e -> onDeletePost());

        Button reportPostBtn = smallIconButton("Report", "M12 2L2 22H22L12 2ZM12 16V18H12.01V16H12ZM12 10V14H12V10H12Z", false);
        reportPostBtn.setOnAction(e -> onReportPost(reportPostBtn));
        runAsync(
                () -> reportSvc.hasReported(p.getId(), currentUserId),
                reportPostBtn::setDisable,
                Throwable::printStackTrace,
                "post-report-status"
        );

        if (p.getAuthorId() != currentUserId) {
            editPostBtn.setVisible(false);
            editPostBtn.setManaged(false);
            deletePostBtn.setVisible(false);
            deletePostBtn.setManaged(false);
        }

        VBox headerRight = new VBox(2, authorLabel, metaRow);
        headerRight.setAlignment(Pos.CENTER_LEFT);
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox headerActions = new HBox(8, reportPostBtn, editPostBtn, deletePostBtn);
        headerActions.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(12, avatar, headerRight, headerSpacer, headerActions);
        header.getStyleClass().add("post-card-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 18, 14, 18));
        getChildren().add(header);

        VBox body = new VBox(10);
        body.getStyleClass().add("post-card-body");
        body.setPadding(new Insets(0, 18, 12, 18));

        Node mediaNode = buildMediaNode();
        if (mediaNode != null) {
            body.getChildren().add(mediaNode);
        }

        captionLabel = new Label(p.getCaption() != null ? p.getCaption() : "");
        captionLabel.getStyleClass().add("post-caption");
        captionLabel.setWrapText(true);
        body.getChildren().add(captionLabel);
        body.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof Button) return;
            openPostDetailsDialog();
        });
        getChildren().add(body);

        likeCountLabel = new Label(String.valueOf(p.getLikesCount()));
        dislikeCountLabel = new Label(String.valueOf(p.getDislikesCount()));
        commentCountLabel = new Label(String.valueOf(p.getCommentsCount()));
        shareCountLabel = new Label(String.valueOf(p.getSharesCount()));
        likeCountLabel.getStyleClass().add("footer-count");
        dislikeCountLabel.getStyleClass().add("footer-count");
        commentCountLabel.getStyleClass().add("footer-count");
        shareCountLabel.getStyleClass().add("footer-count");

        likeBtn = actionButton("Like", "M12 21C12 21 5 14.7 5 9.5C5 7 7 5 9.5 5C10.9 5 12 5.8 12 5.8C12 5.8 13.1 5 14.5 5C17 5 19 7 19 9.5C19 14.7 12 21 12 21Z");
        dislikeBtn = actionButton("Dislike", "M12 3C12 3 19 9.3 19 14.5C19 17 17 19 14.5 19C13.1 19 12 18.2 12 18.2C12 18.2 10.9 19 9.5 19C7 19 5 17 5 14.5C5 9.3 12 3 12 3Z");
        Button commentBtn = actionButton("Comment", "M4 5H20V16H7L4 19V5Z");
        shareBtn = actionButton("Share", "M14 5L20 10L14 15V11H4V9H14V5Z");

        likeBtn.setOnAction(e -> togglePostLike());
        dislikeBtn.setOnAction(e -> togglePostDislike());
        shareBtn.setOnAction(e -> togglePostShare());

        commentBtn.setOnAction(e -> openPostDetailsDialog());

        HBox footer = new HBox(18);
        footer.getStyleClass().add("post-card-footer");
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(8, 18, 12, 18));
        footer.getChildren().addAll(
                likeBtn, likeCountLabel,
                dislikeBtn, dislikeCountLabel,
                commentBtn, commentCountLabel,
                shareBtn, shareCountLabel
        );
        getChildren().add(footer);

        applyPostReactionUi(Reaction.NONE);
        runAsync(
                () -> reactionFromDb(reactionSvc.getReaction(p.getId(), currentUserId)),
                reaction -> {
                    currentPostReaction = reaction;
                    applyPostReactionUi(reaction);
                },
                ex -> {
                    ex.printStackTrace();
                    applyPostReactionUi(Reaction.NONE);
                },
                "post-init-reaction"
        );
        runAsync(
                () -> shareSvc.hasShared(p.getId(), currentUserId),
                this::applyShareUi,
                Throwable::printStackTrace,
                "post-init-share"
        );

        commentsSection = new VBox(12);
        commentsSection.getStyleClass().add("post-comments-section");
        commentsSection.setPadding(new Insets(0, 18, 16, 18));
        commentsSection.setMaxHeight(0);
        Rectangle commentsClip = new Rectangle();
        commentsClip.widthProperty().bind(commentsSection.widthProperty());
        commentsClip.heightProperty().bind(commentsSection.maxHeightProperty());
        commentsSection.setClip(commentsClip);
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
        commentInput.setPromptText("Comment on this post ");
        commentInput.setPrefHeight(36);
        HBox.setHgrow(commentInput, Priority.ALWAYS);

        Button postCommentBtn = new Button("Post");
        postCommentBtn.getStyleClass().add("btn-comment-submit");
        postCommentBtn.setOnAction(e -> {
            String bodyText = commentInput.getText().trim();
            if (bodyText.isEmpty()) return;
            comment c = new comment();
            c.setPostId(p.getId());
            c.setAuthorId(currentUserId);
            c.setBody(bodyText);
            c.setStatus("ACTIVE");
            if (replyToComment != null) {
                c.setParentCommentId(replyToComment.getId());
                replyToComment = null;
                commentInput.setPromptText("Write a comment...");
            } else {
                c.setParentCommentId(null);
            }

            runAsync(
                    () -> {
                        commentSvc.ajouter(c);
                        return true;
                    },
                    ok -> {
                        commentInput.clear();
                        refreshComments();
                        p.setCommentsCount(p.getCommentsCount() + 1);
                        commentCountLabel.setText(String.valueOf(p.getCommentsCount()));
                    },
                    ex -> {
                        ex.printStackTrace();
                        new Alert(Alert.AlertType.ERROR, "Failed to add comment: " + ex.getMessage()).showAndWait();
                    },
                    "post-comment-add"
            );
        });

        HBox inputRow = new HBox(10, commentInput, postCommentBtn);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.getStyleClass().add("comment-input-bar");

        commentsSection.getChildren().addAll(new Separator(), commentsScroll, inputRow);
        getChildren().add(commentsSection);

        this.commentInputRef = commentInput;

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null && outsideClickHandler != null) {
                oldScene.removeEventFilter(MouseEvent.MOUSE_PRESSED, outsideClickHandler);
            }
            if (newScene == null) {
                disposeMediaPlayer();
                outsideClickHandler = null;
            } else if (commentsSection.isVisible()) {
                registerOutsideClickToClose();
            }
        });
    }

    private Node buildMediaNode() {
        String mediaType = p.getMediaType() == null ? "" : p.getMediaType().toUpperCase(Locale.ROOT);
        String mediaPath = p.getMediaPath();
        if (mediaPath == null || mediaPath.isEmpty() || "NONE".equals(mediaType)) {
            return null;
        }

        try {
            String uri = mediaPath.startsWith("file:") ? mediaPath : new File(mediaPath).toURI().toString();
            if ("VIDEO".equals(mediaType) || isVideoPath(mediaPath)) {
                Media media = new Media(uri);
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setAutoPlay(false);

                MediaView mediaView = new MediaView(mediaPlayer);
                mediaView.setFitWidth(FEED_IMAGE_WIDTH);
                mediaView.setFitHeight(FEED_IMAGE_HEIGHT);
                mediaView.setPreserveRatio(true);

                Button playPause = smallIconButton("Play", "M8 6L18 12L8 18V6Z", false);
                playPause.setOnAction(e -> {
                    MediaPlayer.Status status = mediaPlayer.getStatus();
                    if (status == MediaPlayer.Status.PLAYING) {
                        mediaPlayer.pause();
                        playPause.setText("Play");
                    } else {
                        mediaPlayer.play();
                        playPause.setText("Pause");
                    }
                });

                VBox wrap = new VBox(8, new StackPane(mediaView), playPause);
                wrap.getStyleClass().add("post-image-wrap");
                return wrap;
            }

            ImageView imgView = new ImageView(new Image(uri));
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
            return imageWrap;
        } catch (Exception ex) {
            Label fallback = new Label("Media preview unavailable");
            fallback.getStyleClass().add("post-meta");
            return fallback;
        }
    }

    private boolean isVideoPath(String path) {
        String pth = path.toLowerCase(Locale.ROOT);
        return pth.endsWith(".mp4") || pth.endsWith(".mov") || pth.endsWith(".m4v");
    }

    private String getVisibilityIconPath(String visibility) {
        String value = visibility == null ? "" : visibility.toUpperCase(Locale.ROOT);
        return switch (value) {
            case "PRIVATE" -> "M17 8H16V6C16 3.8 14.2 2 12 2C9.8 2 8 3.8 8 6V8H7C5.9 8 5 8.9 5 10V20C5 21.1 5.9 22 7 22H17C18.1 22 19 21.1 19 20V10C19 8.9 18.1 8 17 8ZM10 6C10 4.9 10.9 4 12 4C13.1 4 14 4.9 14 6V8H10V6Z";
            case "FRIENDS" -> "M16 11C17.7 11 19 9.7 19 8S17.7 5 16 5S13 6.3 13 8S14.3 11 16 11ZM8 11C9.7 11 11 9.7 11 8S9.7 5 8 5S5 6.3 5 8S6.3 11 8 11ZM8 13C5.7 13 1 14.1 1 16.5V19H15V16.5C15 14.1 10.3 13 8 13ZM16 13C15.7 13 15.3 13 15 13.1C16.2 14 17 15.2 17 16.5V19H23V16.5C23 14.1 18.3 13 16 13Z";
            default -> "M12 2C7.6 2 4 5.6 4 10C4 16 12 22 12 22S20 16 20 10C20 5.6 16.4 2 12 2ZM6.1 11H9.1C9.2 12.5 9.6 13.9 10.1 15H7.2C6.7 13.8 6.3 12.4 6.1 11ZM6.1 9C6.3 7.6 6.7 6.2 7.2 5H10.1C9.6 6.1 9.2 7.5 9.1 9H6.1ZM17.9 9H14.9C14.8 7.5 14.4 6.1 13.9 5H16.8C17.3 6.2 17.7 7.6 17.9 9ZM12 5C12.6 6.1 13 7.5 13.1 9H10.9C11 7.5 11.4 6.1 12 5ZM10.9 11H13.1C13 12.5 12.6 13.9 12 15C11.4 13.9 11 12.5 10.9 11ZM14.9 11H17.9C17.7 12.4 17.3 13.8 16.8 15H13.9C14.4 13.9 14.8 12.5 14.9 11Z";
        };
    }

    private Button actionButton(String text, String svgPath) {
        Button b = new Button(text);
        b.getStyleClass().add("action-button");
        b.setGraphic(createIcon(svgPath));
        return b;
    }

    private Button smallIconButton(String text, String svgPath, boolean danger) {
        Button b = new Button(text);
        b.getStyleClass().add("action-button-small");
        if (danger) b.getStyleClass().add("danger-button-small");
        b.setGraphic(createIcon(svgPath));
        return b;
    }

    private StackPane createIcon(String svgPathData) {
        SVGPath icon = new SVGPath();
        icon.setContent(svgPathData);
        icon.getStyleClass().add("action-icon-shape");

        StackPane wrap = new StackPane(icon);
        wrap.getStyleClass().add("action-icon-wrap");
        wrap.setMinSize(12, 12);
        wrap.setPrefSize(12, 12);
        wrap.setMaxSize(12, 12);
        return wrap;
    }

    private void togglePostLike() {
        applyPostReaction("LIKE");
    }

    private void togglePostDislike() {
        applyPostReaction("DISLIKE");
    }

    private void applyPostReaction(String desired) {
        Reaction previous = currentPostReaction;
        runAsync(
                () -> reactionFromDb(reactionSvc.toggleReaction(p.getId(), currentUserId, desired)),
                next -> {
                    int likeDelta = reactionToLikeValue(next) - reactionToLikeValue(previous);
                    int dislikeDelta = reactionToDislikeValue(next) - reactionToDislikeValue(previous);
                    p.setLikesCount(Math.max(0, p.getLikesCount() + likeDelta));
                    p.setDislikesCount(Math.max(0, p.getDislikesCount() + dislikeDelta));
                    likeCountLabel.setText(String.valueOf(p.getLikesCount()));
                    dislikeCountLabel.setText(String.valueOf(p.getDislikesCount()));

                    currentPostReaction = next;
                    applyPostReactionUi(next);
                },
                e -> {
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Failed to react: " + e.getMessage()).showAndWait();
                },
                "post-toggle-reaction"
        );
    }

    private int reactionToLikeValue(Reaction reaction) {
        return reaction == Reaction.LIKE ? 1 : 0;
    }

    private int reactionToDislikeValue(Reaction reaction) {
        return reaction == Reaction.DISLIKE ? 1 : 0;
    }

    private Reaction reactionFromDb(String value) {
        if ("LIKE".equalsIgnoreCase(value)) return Reaction.LIKE;
        if ("DISLIKE".equalsIgnoreCase(value)) return Reaction.DISLIKE;
        return Reaction.NONE;
    }

    private void applyPostReactionUi(Reaction reaction) {
        likeBtn.getStyleClass().remove("reaction-active-like");
        dislikeBtn.getStyleClass().remove("reaction-active-dislike");
        if (reaction == Reaction.LIKE) {
            likeBtn.getStyleClass().add("reaction-active-like");
        } else if (reaction == Reaction.DISLIKE) {
            dislikeBtn.getStyleClass().add("reaction-active-dislike");
        }
    }

    private void togglePostShare() {
        runAsync(
                () -> shareSvc.toggleShare(p.getId(), currentUserId),
                nowShared -> {
                    if (nowShared) {
                        p.setSharesCount(p.getSharesCount() + 1);
                        shareCountLabel.setText(String.valueOf(p.getSharesCount()));
                        applyShareUi(true);
                    } else {
                        int next = Math.max(0, p.getSharesCount() - 1);
                        p.setSharesCount(next);
                        shareCountLabel.setText(String.valueOf(next));
                        applyShareUi(false);
                    }
                },
                e -> {
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Failed to share post: " + e.getMessage()).showAndWait();
                },
                "post-toggle-share"
        );
    }

    private void openPostDetailsDialog() {
        PostDetailsDialog details = new PostDetailsDialog(p, postSvc, commentSvc, currentUserId, onRefreshFeed);
        if (getScene() != null && getScene().getWindow() != null) {
            details.initOwner(getScene().getWindow());
        }
        details.showAndWait();
    }

    private void applyShareUi(boolean shared) {
        shareBtn.getStyleClass().remove("reaction-active-share");
        if (shared) {
            shareBtn.getStyleClass().add("reaction-active-share");
        }
    }

    private Reaction getCommentReaction(comment c) {
        CommentReactionService.ReactionSnapshot snapshot = commentReactionCache.get(c.getId());
        if (snapshot == null) {
            return Reaction.NONE;
        }
        return reactionFromDb(snapshot.getReaction());
    }

    private int getCommentLikeCount(comment c) {
        CommentReactionService.ReactionSnapshot snapshot = commentReactionCache.get(c.getId());
        return snapshot == null ? 0 : snapshot.getLikesCount();
    }

    private int getCommentDislikeCount(comment c) {
        CommentReactionService.ReactionSnapshot snapshot = commentReactionCache.get(c.getId());
        return snapshot == null ? 0 : snapshot.getDislikesCount();
    }

    private void toggleCommentLike(comment c) {
        runAsync(
                () -> commentReactionSvc.toggleReaction(c.getId(), currentUserId, "LIKE"),
                snapshot -> {
                    commentReactionCache.put(c.getId(), snapshot);
                    refreshComments();
                },
                ex -> {
                    ex.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Failed to react to comment: " + ex.getMessage()).showAndWait();
                },
                "post-comment-like-toggle"
        );
    }

    private void toggleCommentDislike(comment c) {
        runAsync(
                () -> commentReactionSvc.toggleReaction(c.getId(), currentUserId, "DISLIKE"),
                snapshot -> {
                    commentReactionCache.put(c.getId(), snapshot);
                    refreshComments();
                },
                ex -> {
                    ex.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Failed to react to comment: " + ex.getMessage()).showAndWait();
                },
                "post-comment-dislike-toggle"
        );
    }

    private void toggleComments() {
        if (commentsSection.isVisible()) {
            collapseComments();
            return;
        }

        refreshComments();
        commentsSection.setManaged(true);
        commentsSection.setVisible(true);
        commentsSection.setMaxHeight(0);

        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(commentsSection.maxHeightProperty(), 0)),
                new KeyFrame(Duration.millis(220), new KeyValue(commentsSection.maxHeightProperty(), COMMENT_SECTION_MAX_HEIGHT, Interpolator.EASE_OUT))
        );
        t.play();

        registerOutsideClickToClose();
    }

    private void collapseComments() {
        commentsSection.setMaxHeight(0);
        commentsSection.setVisible(false);
        commentsSection.setManaged(false);
        unregisterOutsideClickToClose();
    }

    private void registerOutsideClickToClose() {
        Scene scene = getScene();
        if (scene == null || outsideClickHandler != null) return;

        outsideClickHandler = event -> {
            if (!commentsSection.isVisible()) return;
            Object targetObj = event.getTarget();
            if (!(targetObj instanceof Node targetNode)) return;
            if (isDescendantOf(targetNode, this)) return;
            collapseComments();
        };

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, outsideClickHandler);
    }

    private void unregisterOutsideClickToClose() {
        Scene scene = getScene();
        if (scene != null && outsideClickHandler != null) {
            scene.removeEventFilter(MouseEvent.MOUSE_PRESSED, outsideClickHandler);
        }
        outsideClickHandler = null;
    }

    private boolean isDescendantOf(Node node, Node possibleAncestor) {
        Node current = node;
        while (current != null) {
            if (current == possibleAncestor) return true;
            current = current.getParent();
        }
        return false;
    }

    private void refreshComments() {
        commentsList.getChildren().clear();
        runAsync(
                () -> {
                    List<comment> flat = commentSvc.findByPostId(p.getId());
                    Map<Long, CommentReactionService.ReactionSnapshot> snapshots = new HashMap<>();
                    for (comment item : flat) {
                        snapshots.put(item.getId(), commentReactionSvc.getSnapshot(item.getId(), currentUserId));
                    }
                    return new CommentRenderData(flat, snapshots);
                },
                data -> {
                    commentReactionCache.clear();
                    commentReactionCache.putAll(data.snapshots);

                    List<comment> flat = data.comments;
                    List<comment> roots = flat.stream().filter(c -> c.getParentCommentId() == null).collect(Collectors.toList());
                    List<comment> withParent = flat.stream().filter(c -> c.getParentCommentId() != null).collect(Collectors.toList());
                    for (comment root : roots) {
                        commentsList.getChildren().add(new CommentItem(
                                root,
                                withParent,
                                false,
                                this::onReplyClicked,
                                this::onEditComment,
                                this::onDeleteComment,
                                this::canManageComment,
                                this::canManageComment,
                                this::getCommentLikeCount,
                                this::getCommentDislikeCount,
                                c -> getCommentReaction(c) == Reaction.LIKE,
                                c -> getCommentReaction(c) == Reaction.DISLIKE,
                                this::toggleCommentLike,
                                this::toggleCommentDislike
                        ));
                    }
                },
                ex -> {
                    ex.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Failed to load comments: " + ex.getMessage()).showAndWait();
                },
                "post-load-comments"
        );
    }

    private void onEditPost() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Post");
        dialog.setHeaderText("Update post caption");
        styleDialog(dialog);

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        TextField captionField = new TextField(p.getCaption() != null ? p.getCaption() : "");
        captionField.getStyleClass().add("dialog-input");
        captionField.setPromptText("Write a caption...");
        dialog.getDialogPane().setContent(captionField);

        dialog.setResultConverter(bt -> bt == saveType ? captionField.getText() : null);
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        String updatedCaption = result.get().trim();
        if (updatedCaption.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Caption cannot be empty.").showAndWait();
            return;
        }

        p.setCaption(updatedCaption);
        runAsync(
                () -> {
                    postSvc.modifier(p);
                    return true;
                },
                ok -> {
                    captionLabel.setText(updatedCaption);
                    if (onRefreshFeed != null) onRefreshFeed.run();
                },
                e -> {
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Failed to update post: " + e.getMessage()).showAndWait();
                },
                "post-edit"
        );
    }

    private void onDeletePost() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this post and all its comments?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Delete Post");
        confirm.setHeaderText("Confirm post deletion");
        styleDialog(confirm);
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.YES) return;

        runAsync(
                () -> {
                    commentSvc.deleteByPostId(p.getId());
                    postSvc.supprimer(p.getId());
                    return true;
                },
                ok -> {
                    if (onRefreshFeed != null) onRefreshFeed.run();
                },
                e -> {
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Failed to delete post: " + e.getMessage()).showAndWait();
                },
                "post-delete"
        );
    }

    private void onEditComment(comment c) {
        if (!canManageComment(c)) {
            new Alert(Alert.AlertType.WARNING, "You can only edit your own comment unless you own this post.").showAndWait();
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Comment");
        dialog.setHeaderText("Update comment text");
        styleDialog(dialog);

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        TextField bodyField = new TextField(c.getBody() != null ? c.getBody() : "");
        bodyField.getStyleClass().add("dialog-input");
        bodyField.setPromptText("Write your comment...");
        dialog.getDialogPane().setContent(bodyField);

        dialog.setResultConverter(bt -> bt == saveType ? bodyField.getText() : null);
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        String updatedBody = result.get().trim();
        if (updatedBody.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Comment cannot be empty.").showAndWait();
            return;
        }

        c.setBody(updatedBody);
        if (c.getStatus() == null) c.setStatus("ACTIVE");
        runAsync(
                () -> commentSvc.updateComment(c, currentUserId),
                updated -> {
                    if (!updated) {
                        new Alert(Alert.AlertType.WARNING, "Not allowed to edit this comment.").showAndWait();
                        return;
                    }
                    refreshComments();
                },
                e -> {
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Failed to update comment: " + e.getMessage()).showAndWait();
                },
                "post-comment-edit"
        );
    }

    private void onDeleteComment(comment c) {
        if (!canManageComment(c)) {
            new Alert(Alert.AlertType.WARNING, "You can only delete your own comment unless you own this post.").showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this comment?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Delete Comment");
        confirm.setHeaderText("Confirm comment deletion");
        styleDialog(confirm);
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.YES) return;

        runAsync(
                () -> commentSvc.deleteComment(c.getId(), currentUserId),
                deleted -> {
                    if (!deleted) {
                        new Alert(Alert.AlertType.WARNING, "Not allowed to delete this comment.").showAndWait();
                        return;
                    }
                    commentReactionCache.remove(c.getId());
                    refreshComments();
                    int count = Math.max(0, p.getCommentsCount() - 1);
                    p.setCommentsCount(count);
                    commentCountLabel.setText(String.valueOf(count));
                    if (commentInputRef != null && replyToComment != null && replyToComment.getId() == c.getId()) {
                        replyToComment = null;
                        commentInputRef.setPromptText("Write a comment...");
                    }
                },
                ex -> {
                    ex.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Failed to delete comment: " + ex.getMessage()).showAndWait();
                },
                "post-comment-delete"
        );
    }

    private boolean canManageComment(comment c) {
        if (c == null) {
            return false;
        }
        return currentUserId == c.getAuthorId() || currentUserId == p.getAuthorId();
    }

    private void onReplyClicked(comment parent) {
        replyToComment = parent;
        if (commentInputRef != null) {
            String authorDisplayName = (parent.getAuthorName() != null && !parent.getAuthorName().isBlank())
                    ? parent.getAuthorName()
                    : ("User " + parent.getAuthorId());
            commentInputRef.setPromptText("Reply to " + authorDisplayName + "...");
            commentInputRef.requestFocus();
        }
    }

    private void onReportPost(Button reportPostBtn) {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Report Post");
        dialog.setHeaderText("Report this post");
        dialog.setContentText("Reason (optional):");
        styleDialog(dialog);

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        String reason = result.get();
        runAsync(
                () -> reportSvc.reportPost(p.getId(), currentUserId, reason),
                reported -> {
                    if (reported) {
                        new Alert(Alert.AlertType.INFORMATION, "Reported").showAndWait();
                        reportPostBtn.setDisable(true);
                    } else {
                        new Alert(Alert.AlertType.INFORMATION, "Already reported").showAndWait();
                    }
                },
                e -> {
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Failed to report post: " + e.getMessage()).showAndWait();
                },
                "post-report"
        );
    }

    private void disposeMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    private void styleDialog(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("app-dialog-pane");
        if (getClass().getResource("/css/app.css") != null) {
            pane.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        }
    }

    public post getPost() {
        return p;
    }

    public long getPostId() {
        return p.getId();
    }

    public void playFocusTransition() {
        DropShadow focusGlow = new DropShadow();
        focusGlow.setColor(Color.web("#4e9de6"));
        focusGlow.setRadius(20);
        focusGlow.setSpread(0.35);

        setEffect(focusGlow);

        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(scaleYProperty(), 1.0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(220),
                        new KeyValue(scaleXProperty(), 1.02, Interpolator.EASE_BOTH),
                        new KeyValue(scaleYProperty(), 1.02, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(520),
                        new KeyValue(scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(scaleYProperty(), 1.0, Interpolator.EASE_BOTH))
        );
        pulse.setOnFinished(event -> setEffect(null));
        pulse.play();
    }

    private static class CommentRenderData {
        private final List<comment> comments;
        private final Map<Long, CommentReactionService.ReactionSnapshot> snapshots;

        private CommentRenderData(List<comment> comments, Map<Long, CommentReactionService.ReactionSnapshot> snapshots) {
            this.comments = comments;
            this.snapshots = snapshots;
        }
    }

    private <T> void runAsync(Callable<T> action, Consumer<T> onSuccess, Consumer<Throwable> onError, String threadName) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return action.call();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> onError.accept(task.getException()));
        Thread thread = new Thread(task, threadName == null ? "postcard-jdbc-task" : threadName);
        thread.setDaemon(true);
        thread.start();
    }
}
