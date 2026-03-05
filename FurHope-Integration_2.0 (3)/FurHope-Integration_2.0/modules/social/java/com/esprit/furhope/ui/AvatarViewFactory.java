package com.esprit.furhope.ui;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AvatarViewFactory {

    private static final Map<String, Image> IMAGE_CACHE = new ConcurrentHashMap<>();

    private AvatarViewFactory() {
    }

    public static StackPane createAvatar(String imagePath, String displayName, double size, String styleClass) {
        StackPane container = new StackPane();
        if (styleClass != null && !styleClass.isBlank()) {
            container.getStyleClass().add(styleClass);
        }
        container.getStyleClass().add("avatar-shell");
        container.setMinSize(size, size);
        container.setPrefSize(size, size);
        container.setMaxSize(size, size);

        Image image = resolveImage(imagePath);
        if (image != null && !image.isError()) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(size);
            imageView.setFitHeight(size);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);
            imageView.getStyleClass().add("avatar-image-fill");
            imageView.setClip(new Circle(size / 2, size / 2, size / 2));
            container.getChildren().add(imageView);
            return container;
        }

        Label initials = new Label(toInitials(displayName));
        initials.getStyleClass().add("avatar-initials");
        container.getChildren().add(initials);
        return container;
    }

    private static Image resolveImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        String normalized = normalizePath(imagePath);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        return IMAGE_CACHE.computeIfAbsent(normalized, key -> new Image(key, true));
    }

    private static String normalizePath(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file:")) {
            return trimmed;
        }
        File file = new File(trimmed);
        if (!file.exists()) {
            return null;
        }
        return file.toURI().toString();
    }

    private static String toInitials(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "?";
        }
        String[] parts = displayName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        String first = parts[0].substring(0, 1).toUpperCase();
        String second = parts[1].substring(0, 1).toUpperCase();
        return first + second;
    }
}
