package com.esprit.furhope.ui;

import com.esprit.furhope.entities.comment;
import com.esprit.furhope.entities.post;
import com.esprit.furhope.services.CommentServiceJdbc;
import com.esprit.furhope.services.CommentReactionService;
import com.esprit.furhope.services.PostServiceJdbc;
import com.esprit.furhope.utils.TimeUtils;
import javafx.concurrent.Task;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

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

public class PostDetailsDialog extends Stage {

    private enum Reaction {
        NONE,
        LIKE,
        DISLIKE
    }

    private static final int MEDIA_WIDTH = 720;
    private static final int MEDIA_HEIGHT = 420;

    private final post p;
    private final PostServiceJdbc postSvc;
    private final CommentServiceJdbc commentSvc;
    private final CommentReactionService commentReactionSvc;
    private final int currentUserId;
    private final Runnable onRefreshFeed;

    private final VBox commentsList = new VBox(8);
    private final Map<Long, CommentReactionService.ReactionSnapshot> commentReactionCache = new ConcurrentHashMap<>();

    private TextArea commentInput;
    private comment replyToComment;
    private MediaPlayer mediaPlayer;

    public PostDetailsDialog(post p, PostServiceJdbc postSvc, CommentServiceJdbc commentSvc, int currentUserId, Runnable onRefreshFeed) {
        this.p = p;
        this.postSvc = postSvc;
        this.commentSvc = commentSvc;
        this.commentReactionSvc = new CommentReactionService();
        this.currentUserId = currentUserId;
        this.onRefreshFeed = onRefreshFeed;

        Window owner = null;
        if (onRefreshFeed != null) {
            // no-op; owner may be set by caller via initOwner if needed
        }
        initModality(Modality.APPLICATION_MODAL);
        if (owner != null) initOwner(owner);
        setTitle("Post Details");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: #f8fbff;");

        root.setTop(buildHeader());
        root.setCenter(buildContentCenter());
        root.setBottom(buildComposer());

        Scene scene = new Scene(root, 840, 900);
        if (getClass().getResource("/css/app.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        }
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });
        setScene(scene);
        setWidth(900);
        setHeight(750);

        setOnHidden(e -> disposeMediaPlayer());
        refreshComments();
    }

    private Node buildHeader() {
        String authorDisplayName = (p.getAuthorName() != null && !p.getAuthorName().isBlank())
                ? p.getAuthorName()
                : ("User " + p.getAuthorId());
        Node avatar = AvatarViewFactory.createAvatar(
                p.getAuthorProfileImagePath(),
                authorDisplayName,
                42,
                "post-avatar"
        );
        Label authorLabel = new Label(authorDisplayName);
        authorLabel.getStyleClass().add("post-author");

        Label timeLabel = new Label(TimeUtils.formatAgo(p.getCreatedAt()));
        timeLabel.getStyleClass().add("post-meta");

        SVGPath visibilityIcon = new SVGPath();
        visibilityIcon.setContent(getVisibilityIconPath(p.getVisibility()));
        visibilityIcon.fillProperty().bind(timeLabel.textFillProperty());
        StackPane visibilityWrap = new StackPane(visibilityIcon);
        visibilityWrap.setMinSize(15, 15);
        visibilityWrap.setPrefSize(15, 15);
        visibilityWrap.setMaxSize(15, 15);

        HBox meta = new HBox(6, timeLabel, visibilityWrap);
        meta.setAlignment(Pos.CENTER_LEFT);
        VBox left = new VBox(2, authorLabel, meta);

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("composer-btn-secondary");
        closeBtn.setOnAction(e -> close());

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, avatar, left, spacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));
        return header;
    }

    private Node buildContentCenter() {
        BorderPane center = new BorderPane();
        center.setPadding(new Insets(0));

        VBox topContent = new VBox(10);
        topContent.setFillWidth(true);
        Node mediaNode = buildMediaNode();
        if (mediaNode != null) {
            topContent.getChildren().add(mediaNode);
        }

        Label caption = new Label(p.getCaption() != null ? p.getCaption() : "");
        caption.getStyleClass().add("post-caption");
        caption.setWrapText(true);
        topContent.getChildren().add(caption);

        Label commentsTitle = new Label("Comments");
        commentsTitle.getStyleClass().add("composer-section-title");
        topContent.getChildren().addAll(new Separator(), commentsTitle);
        center.setTop(topContent);

        commentsList.setFillWidth(true);
        ScrollPane commentsScroll = new ScrollPane(commentsList);
        commentsScroll.setFitToWidth(true);
        commentsScroll.setPannable(true);
        commentsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        center.setCenter(commentsScroll);
        return center;
    }

    private Node buildComposer() {
        commentInput = new TextArea();
        commentInput.setPromptText("Write a comment...");
        commentInput.setWrapText(true);
        commentInput.setPrefRowCount(3);
        commentInput.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                submitComment();
            }
        });

        Button postBtn = new Button("Post");
        postBtn.getStyleClass().add("composer-btn-primary");
        postBtn.setOnAction(e -> submitComment());

        HBox footer = new HBox(10, commentInput, postBtn);
        footer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(commentInput, Priority.ALWAYS);
        footer.setPadding(new Insets(12, 0, 0, 0));
        return footer;
    }

    private void submitComment() {
        String bodyText = commentInput.getText() == null ? "" : commentInput.getText().trim();
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
                    if (onRefreshFeed != null) onRefreshFeed.run();
                },
                ex -> {
                    ex.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Failed to add comment: " + ex.getMessage()).showAndWait();
                },
                "details-comment-add"
        );
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
                e -> {
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Failed to load comments: " + e.getMessage()).showAndWait();
                    commentsList.getChildren().setAll(new Label("Failed to load comments."));
                },
                "details-comments-load"
        );
    }

    private void onReplyClicked(comment parent) {
        replyToComment = parent;
        commentInput.setPromptText("Reply to " + (parent.getAuthorName() != null ? parent.getAuthorName() : ("User " + parent.getAuthorId())) + "...");
        commentInput.requestFocus();
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
                "details-comment-edit"
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
                    if (onRefreshFeed != null) onRefreshFeed.run();
                    if (replyToComment != null && replyToComment.getId() == c.getId()) {
                        replyToComment = null;
                        commentInput.setPromptText("Write a comment...");
                    }
                },
                e -> {
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Failed to delete comment: " + e.getMessage()).showAndWait();
                },
                "details-comment-delete"
        );
    }

    private boolean canManageComment(comment c) {
        if (c == null) {
            return false;
        }
        return currentUserId == c.getAuthorId() || currentUserId == p.getAuthorId();
    }

    private int getCommentLikeCount(comment c) {
        CommentReactionService.ReactionSnapshot snapshot = commentReactionCache.get(c.getId());
        return snapshot == null ? 0 : snapshot.getLikesCount();
    }

    private int getCommentDislikeCount(comment c) {
        CommentReactionService.ReactionSnapshot snapshot = commentReactionCache.get(c.getId());
        return snapshot == null ? 0 : snapshot.getDislikesCount();
    }

    private Reaction getCommentReaction(comment c) {
        CommentReactionService.ReactionSnapshot snapshot = commentReactionCache.get(c.getId());
        if (snapshot == null) {
            return Reaction.NONE;
        }
        return reactionFromDb(snapshot.getReaction());
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
                "details-comment-like-toggle"
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
                "details-comment-dislike-toggle"
        );
    }

    private Reaction reactionFromDb(String value) {
        if ("LIKE".equalsIgnoreCase(value)) return Reaction.LIKE;
        if ("DISLIKE".equalsIgnoreCase(value)) return Reaction.DISLIKE;
        return Reaction.NONE;
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
                mediaView.setFitWidth(MEDIA_WIDTH);
                mediaView.setFitHeight(MEDIA_HEIGHT);
                mediaView.setPreserveRatio(true);

                Button playPause = new Button("Play");
                playPause.getStyleClass().add("composer-btn-secondary");
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
            imgView.setFitWidth(MEDIA_WIDTH);
            imgView.setFitHeight(MEDIA_HEIGHT);
            imgView.setPreserveRatio(true);
            imgView.setSmooth(true);
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

    private <T> void runAsync(Callable<T> action, Consumer<T> onSuccess, Consumer<Throwable> onError, String threadName) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return action.call();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> onError.accept(task.getException()));
        Thread thread = new Thread(task, threadName == null ? "post-details-jdbc-task" : threadName);
        thread.setDaemon(true);
        thread.start();
    }

    private static class CommentRenderData {
        private final List<comment> comments;
        private final Map<Long, CommentReactionService.ReactionSnapshot> snapshots;

        private CommentRenderData(List<comment> comments, Map<Long, CommentReactionService.ReactionSnapshot> snapshots) {
            this.comments = comments;
            this.snapshots = snapshots;
        }
    }
}
