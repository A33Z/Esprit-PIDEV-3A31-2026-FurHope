package com.esprit.furhope.ui;

import com.esprit.furhope.entities.comment;
import com.esprit.furhope.utils.TimeUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class CommentItem extends VBox {

    private static final int REPLY_INDENT = 24;

    private final comment c;

    public CommentItem(comment c,
                       List<comment> replies,
                       boolean isReply,
                       Consumer<comment> onReplyClicked,
                       Consumer<comment> onEditClicked,
                       Consumer<comment> onDeleteClicked,
                       Predicate<comment> canEdit,
                       Predicate<comment> canDelete,
                       Function<comment, Integer> likeCountProvider,
                       Function<comment, Integer> dislikeCountProvider,
                       Predicate<comment> isLiked,
                       Predicate<comment> isDisliked,
                       Consumer<comment> onLikeToggle,
                       Consumer<comment> onDislikeToggle) {

        this.c = c;
        getStyleClass().add(isReply ? "reply-item" : "comment-item");

        setSpacing(6);
        setPadding(new Insets(isReply ? 10 : 12, isReply ? REPLY_INDENT : 16, 12, isReply ? REPLY_INDENT : 16));
        if (isReply) {
            setStyle("-fx-background-color: #f4f7fb; -fx-background-radius: 10;");
        }

        String authorDisplayName = (c.getAuthorName() != null && !c.getAuthorName().isBlank())
                ? c.getAuthorName()
                : ("User " + c.getAuthorId());
        Node avatar = AvatarViewFactory.createAvatar(
                c.getAuthorProfileImagePath(),
                authorDisplayName,
                28,
                "comment-avatar"
        );

        Label authorLabel = new Label(authorDisplayName);
        authorLabel.getStyleClass().add("comment-author");
        Label timeLabel = new Label(TimeUtils.formatAgo(c.getCreatedAt()));
        timeLabel.getStyleClass().add("comment-time");

        HBox metaRow = new HBox(8, avatar, authorLabel, timeLabel);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label bodyLabel = new Label(c.getBody() != null ? c.getBody() : "");
        bodyLabel.getStyleClass().add("comment-body");
        bodyLabel.setWrapText(true);

        Label likeCountLabel = new Label(String.valueOf(likeCountProvider.apply(c)));
        likeCountLabel.getStyleClass().add("comment-count");
        Label dislikeCountLabel = new Label(String.valueOf(dislikeCountProvider.apply(c)));
        dislikeCountLabel.getStyleClass().add("comment-count");

        Button likeBtn = smallIconButton("Like", "M12 21C12 21 5 14.7 5 9.5C5 7 7 5 9.5 5C10.9 5 12 5.8 12 5.8C12 5.8 13.1 5 14.5 5C17 5 19 7 19 9.5C19 14.7 12 21 12 21Z", false);
        Button dislikeBtn = smallIconButton("Dislike", "M12 3C12 3 19 9.3 19 14.5C19 17 17 19 14.5 19C13.1 19 12 18.2 12 18.2C12 18.2 10.9 19 9.5 19C7 19 5 17 5 14.5C5 9.3 12 3 12 3Z", false);
        Button replyBtn = smallIconButton("Reply", "M4 10L12 4V8H17C19.2 8 21 9.8 21 12V16H19V12C19 10.9 18.1 10 17 10H12V14L4 10Z", false);
        Button editBtn = smallIconButton("Edit", "M4 17.5V20H6.5L16.8 9.7L14.3 7.2L4 17.5Z", false);
        Button deleteBtn = smallIconButton("Delete", "M6 7H18V9H17L16 20H8L7 9H6V7Z", true);

        likeBtn.setOnAction(e -> onLikeToggle.accept(c));

        dislikeBtn.setOnAction(e -> onDislikeToggle.accept(c));

        replyBtn.setOnAction(e -> onReplyClicked.accept(c));
        editBtn.setOnAction(e -> onEditClicked.accept(c));
        deleteBtn.setOnAction(e -> onDeleteClicked.accept(c));

        boolean allowEdit = canEdit.test(c);
        boolean allowDelete = canDelete.test(c);
        editBtn.setManaged(allowEdit);
        editBtn.setVisible(allowEdit);
        deleteBtn.setManaged(allowDelete);
        deleteBtn.setVisible(allowDelete);

        applyReactionUi(likeBtn, dislikeBtn, isLiked.test(c), isDisliked.test(c));

        HBox actionsRow = new HBox(12, likeBtn, likeCountLabel, dislikeBtn, dislikeCountLabel, replyBtn, editBtn, deleteBtn);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(metaRow, bodyLabel, actionsRow);

        if (replies != null && !replies.isEmpty()) {
            List<comment> directReplies = replies.stream()
                    .filter(reply -> reply.getParentCommentId() != null && reply.getParentCommentId() == c.getId())
                    .toList();
            if (directReplies.isEmpty()) return;

            VBox repliesContainer = new VBox(6);
            repliesContainer.setPadding(new Insets(8, 0, 0, REPLY_INDENT));
            for (comment reply : directReplies) {
                repliesContainer.getChildren().add(new CommentItem(
                        reply,
                        replies,
                        true,
                        onReplyClicked,
                        onEditClicked,
                        onDeleteClicked,
                        canEdit,
                        canDelete,
                        likeCountProvider,
                        dislikeCountProvider,
                        isLiked,
                        isDisliked,
                        onLikeToggle,
                        onDislikeToggle
                ));
            }
            getChildren().add(repliesContainer);
        }
    }

    private Button smallIconButton(String text, String svgPathData, boolean danger) {
        Button b = new Button(text);
        b.getStyleClass().add("action-button-small");
        if (danger) b.getStyleClass().add("danger-button-small");
        b.setGraphic(createIcon(svgPathData));
        return b;
    }

    private StackPane createIcon(String svgPathData) {
        SVGPath icon = new SVGPath();
        icon.setContent(svgPathData);
        icon.getStyleClass().add("action-icon-shape");

        StackPane wrap = new StackPane(icon);
        wrap.getStyleClass().add("action-icon-wrap");
        wrap.setMinSize(11, 11);
        wrap.setPrefSize(11, 11);
        wrap.setMaxSize(11, 11);
        return wrap;
    }

    private void applyReactionUi(Button likeBtn, Button dislikeBtn, boolean liked, boolean disliked) {
        likeBtn.getStyleClass().remove("reaction-active-like");
        dislikeBtn.getStyleClass().remove("reaction-active-dislike");
        if (liked) likeBtn.getStyleClass().add("reaction-active-like");
        if (disliked) dislikeBtn.getStyleClass().add("reaction-active-dislike");
    }

    public comment getComment() {
        return c;
    }
}
