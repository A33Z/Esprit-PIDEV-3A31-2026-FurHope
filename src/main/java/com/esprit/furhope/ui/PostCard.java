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
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final Map<String, Reaction> POST_REACTIONS = new ConcurrentHashMap<>();
    private static final Map<String, Reaction> COMMENT_REACTIONS = new ConcurrentHashMap<>();
    private static final Map<Long, Integer> COMMENT_LIKE_COUNTS = new ConcurrentHashMap<>();
    private static final Map<Long, Integer> COMMENT_DISLIKE_COUNTS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> POST_SHARES = new ConcurrentHashMap<>();

    private final post p;
    private final postService postSvc;
    private final commentService commentSvc;
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

    public PostCard(post p, postService postSvc, commentService commentSvc, Runnable onRefreshFeed, int currentUserId) {
        this.p = p;
        this.postSvc = postSvc;
        this.commentSvc = commentSvc;
        this.onRefreshFeed = onRefreshFeed;
        this.currentUserId = currentUserId;
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

        Button editPostBtn = smallIconButton("Edit", "M4 17.5V20H6.5L16.8 9.7L14.3 7.2L4 17.5ZM18.7 7.8C19.1 7.4 19.1 6.8 18.7 6.4L17.6 5.3C17.2 4.9 16.6 4.9 16.2 5.3L15.1 6.4L17.6 8.9L18.7 7.8Z", false);
        editPostBtn.setOnAction(e -> onEditPost());

        Button deletePostBtn = smallIconButton("Delete", "M6 7H18V9H17L16 20H8L7 9H6V7ZM9 5H15V6H9V5Z", true);
        deletePostBtn.setOnAction(e -> onDeletePost());

        VBox headerRight = new VBox(2, authorLabel, timeLabel);
        headerRight.setAlignment(Pos.CENTER_LEFT);
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox headerActions = new HBox(8, editPostBtn, deletePostBtn);
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

        commentBtn.setOnAction(e -> toggleComments());

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

        applyPostReactionUi(getPostReaction());
        applyShareUi(isPostShared());

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
                commentSvc.ajouter(c);
                commentInput.clear();
                refreshComments();
                p.setCommentsCount(p.getCommentsCount() + 1);
                commentCountLabel.setText(String.valueOf(p.getCommentsCount()));
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
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
        Reaction current = getPostReaction();
        int likeDelta = 0;
        int dislikeDelta = 0;
        Reaction next;

        if (current == Reaction.LIKE) {
            next = Reaction.NONE;
            likeDelta = -1;
        } else if (current == Reaction.DISLIKE) {
            next = Reaction.LIKE;
            likeDelta = 1;
            dislikeDelta = -1;
        } else {
            next = Reaction.LIKE;
            likeDelta = 1;
        }

        applyPostReaction(next, likeDelta, dislikeDelta);
    }

    private void togglePostDislike() {
        Reaction current = getPostReaction();
        int likeDelta = 0;
        int dislikeDelta = 0;
        Reaction next;

        if (current == Reaction.DISLIKE) {
            next = Reaction.NONE;
            dislikeDelta = -1;
        } else if (current == Reaction.LIKE) {
            next = Reaction.DISLIKE;
            likeDelta = -1;
            dislikeDelta = 1;
        } else {
            next = Reaction.DISLIKE;
            dislikeDelta = 1;
        }

        applyPostReaction(next, likeDelta, dislikeDelta);
    }

    private void applyPostReaction(Reaction next, int likeDelta, int dislikeDelta) {
        try {
            if (likeDelta != 0) postSvc.incrementLikes(p.getId(), likeDelta);
            if (dislikeDelta != 0) postSvc.incrementDislikes(p.getId(), dislikeDelta);

            p.setLikesCount(Math.max(0, p.getLikesCount() + likeDelta));
            p.setDislikesCount(Math.max(0, p.getDislikesCount() + dislikeDelta));
            likeCountLabel.setText(String.valueOf(p.getLikesCount()));
            dislikeCountLabel.setText(String.valueOf(p.getDislikesCount()));

            setPostReaction(next);
            applyPostReactionUi(next);
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
        if (isPostShared()) return;
        try {
            postSvc.incrementShares(p.getId(), 1);
            p.setSharesCount(p.getSharesCount() + 1);
            shareCountLabel.setText(String.valueOf(p.getSharesCount()));
            setPostShared(true);
            applyShareUi(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void applyShareUi(boolean shared) {
        shareBtn.getStyleClass().remove("reaction-active-share");
        if (shared) {
            shareBtn.getStyleClass().add("reaction-active-share");
        }
    }

    private String postReactionKey() {
        return currentUserId + ":post:" + p.getId();
    }

    private Reaction getPostReaction() {
        return POST_REACTIONS.getOrDefault(postReactionKey(), Reaction.NONE);
    }

    private void setPostReaction(Reaction reaction) {
        POST_REACTIONS.put(postReactionKey(), reaction);
    }

    private String postShareKey() {
        return currentUserId + ":share:" + p.getId();
    }

    private boolean isPostShared() {
        return POST_SHARES.getOrDefault(postShareKey(), false);
    }

    private void setPostShared(boolean shared) {
        POST_SHARES.put(postShareKey(), shared);
    }

    private Reaction getCommentReaction(comment c) {
        return COMMENT_REACTIONS.getOrDefault(currentUserId + ":comment:" + c.getId(), Reaction.NONE);
    }

    private void setCommentReaction(comment c, Reaction reaction) {
        COMMENT_REACTIONS.put(currentUserId + ":comment:" + c.getId(), reaction);
    }

    private int getCommentLikeCount(comment c) {
        return COMMENT_LIKE_COUNTS.getOrDefault(c.getId(), 0);
    }

    private int getCommentDislikeCount(comment c) {
        return COMMENT_DISLIKE_COUNTS.getOrDefault(c.getId(), 0);
    }

    private void toggleCommentLike(comment c) {
        Reaction current = getCommentReaction(c);
        int likes = getCommentLikeCount(c);
        int dislikes = getCommentDislikeCount(c);

        if (current == Reaction.LIKE) {
            likes = Math.max(0, likes - 1);
            setCommentReaction(c, Reaction.NONE);
        } else if (current == Reaction.DISLIKE) {
            dislikes = Math.max(0, dislikes - 1);
            likes += 1;
            setCommentReaction(c, Reaction.LIKE);
        } else {
            likes += 1;
            setCommentReaction(c, Reaction.LIKE);
        }

        COMMENT_LIKE_COUNTS.put(c.getId(), likes);
        COMMENT_DISLIKE_COUNTS.put(c.getId(), dislikes);
        refreshComments();
    }

    private void toggleCommentDislike(comment c) {
        Reaction current = getCommentReaction(c);
        int likes = getCommentLikeCount(c);
        int dislikes = getCommentDislikeCount(c);

        if (current == Reaction.DISLIKE) {
            dislikes = Math.max(0, dislikes - 1);
            setCommentReaction(c, Reaction.NONE);
        } else if (current == Reaction.LIKE) {
            likes = Math.max(0, likes - 1);
            dislikes += 1;
            setCommentReaction(c, Reaction.DISLIKE);
        } else {
            dislikes += 1;
            setCommentReaction(c, Reaction.DISLIKE);
        }

        COMMENT_LIKE_COUNTS.put(c.getId(), likes);
        COMMENT_DISLIKE_COUNTS.put(c.getId(), dislikes);
        refreshComments();
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
        try {
            List<comment> flat = commentSvc.findByPostId(p.getId());
            List<comment> roots = flat.stream().filter(c -> c.getParentCommentId() == null).collect(Collectors.toList());
            List<comment> withParent = flat.stream().filter(c -> c.getParentCommentId() != null).collect(Collectors.toList());
            for (comment root : roots) {
                List<comment> replies = withParent.stream()
                        .filter(c -> root.getId() == c.getParentCommentId())
                        .collect(Collectors.toList());

                commentsList.getChildren().add(new CommentItem(
                        root,
                        replies,
                        false,
                        this::onReplyClicked,
                        this::onEditComment,
                        this::onDeleteComment,
                        this::getCommentLikeCount,
                        this::getCommentDislikeCount,
                        c -> getCommentReaction(c) == Reaction.LIKE,
                        c -> getCommentReaction(c) == Reaction.DISLIKE,
                        this::toggleCommentLike,
                        this::toggleCommentDislike
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

        try {
            p.setCaption(updatedCaption);
            postSvc.modifier(p);
            captionLabel.setText(updatedCaption);
            if (onRefreshFeed != null) onRefreshFeed.run();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to update post: " + e.getMessage()).showAndWait();
        }
    }

    private void onDeletePost() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this post and all its comments?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Delete Post");
        confirm.setHeaderText("Confirm post deletion");
        styleDialog(confirm);
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.YES) return;

        try {
            commentSvc.deleteByPostId(p.getId());
            postSvc.supprimer(p.getId());
            if (onRefreshFeed != null) onRefreshFeed.run();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to delete post: " + e.getMessage()).showAndWait();
        }
    }

    private void onEditComment(comment c) {
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

        try {
            c.setBody(updatedBody);
            if (c.getStatus() == null) c.setStatus("ACTIVE");
            commentSvc.modifier(c);
            refreshComments();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to update comment: " + e.getMessage()).showAndWait();
        }
    }

    private void onDeleteComment(comment c) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this comment?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Delete Comment");
        confirm.setHeaderText("Confirm comment deletion");
        styleDialog(confirm);
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.YES) return;

        try {
            commentSvc.supprimer(c.getId());
            COMMENT_LIKE_COUNTS.remove(c.getId());
            COMMENT_DISLIKE_COUNTS.remove(c.getId());
            COMMENT_REACTIONS.remove(currentUserId + ":comment:" + c.getId());
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
}
