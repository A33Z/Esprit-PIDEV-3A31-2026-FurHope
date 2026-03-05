package com.esprit.utils;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class ThemeManager {

    private static final String DARK_MODE_CLASS = "dark-mode";
    private static final String UNIFIED_THEME_STYLESHEET = resolveStylesheet("/css/unified-theme.css");
    private static final String MODERN_THEME_STYLESHEET = resolveStylesheet("/css/theme.css");
    private static final String MODERN_UI_CLASS = "modern-ui";
    private static final double DEFAULT_WIDTH = 1440;
    private static final double DEFAULT_HEIGHT = 900;
    private static final double MIN_WIDTH = 1200;
    private static final double MIN_HEIGHT = 760;
    private static boolean darkModeEnabled = false;

    private ThemeManager() {
    }

    public static void applyToScene(Scene scene) {
        if (scene == null) {
            return;
        }
        applyUnifiedTheme(scene);

        Parent root = scene.getRoot();
        if (root == null) {
            return;
        }

        applyModernTheme(scene, root);

        if (darkModeEnabled) {
            if (!root.getStyleClass().contains(DARK_MODE_CLASS)) {
                root.getStyleClass().add(DARK_MODE_CLASS);
            }
        } else {
            root.getStyleClass().remove(DARK_MODE_CLASS);
        }
    }

    public static void applyToStage(Stage stage) {
        if (stage == null) {
            return;
        }

        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);

        if (stage.getWidth() < DEFAULT_WIDTH) {
            stage.setWidth(DEFAULT_WIDTH);
        }
        if (stage.getHeight() < DEFAULT_HEIGHT) {
            stage.setHeight(DEFAULT_HEIGHT);
        }
    }

    public static void bindStage(Stage stage) {
        if (stage == null) {
            return;
        }

        stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                return;
            }
            applyToScene(newScene);
            Platform.runLater(() -> applyToStage(stage));
        });
    }

    public static void toggle(Scene scene) {
        darkModeEnabled = !darkModeEnabled;
        applyToScene(scene);
    }

    public static boolean isDarkModeEnabled() {
        return darkModeEnabled;
    }

    private static void applyUnifiedTheme(Scene scene) {
        if (scene == null || UNIFIED_THEME_STYLESHEET == null || UNIFIED_THEME_STYLESHEET.isBlank()) {
            return;
        }
        if (!scene.getStylesheets().contains(UNIFIED_THEME_STYLESHEET)) {
            scene.getStylesheets().add(UNIFIED_THEME_STYLESHEET);
        }
    }

    private static void applyModernTheme(Scene scene, Parent root) {
        if (scene == null || root == null || MODERN_THEME_STYLESHEET == null || MODERN_THEME_STYLESHEET.isBlank()) {
            return;
        }

        if (isSocialFeedScene(root)) {
            scene.getStylesheets().remove(MODERN_THEME_STYLESHEET);
            root.getStyleClass().remove(MODERN_UI_CLASS);
            return;
        }

        if (!scene.getStylesheets().contains(MODERN_THEME_STYLESHEET)) {
            scene.getStylesheets().add(MODERN_THEME_STYLESHEET);
        }
        if (!root.getStyleClass().contains(MODERN_UI_CLASS)) {
            root.getStyleClass().add(MODERN_UI_CLASS);
        }
    }

    private static boolean isSocialFeedScene(Parent root) {
        if (root == null) {
            return false;
        }
        return root.getStyleClass().contains("app-root") || root.getStyleClass().contains("feed-root");
    }

    private static String resolveStylesheet(String resourcePath) {
        try {
            var resource = ThemeManager.class.getResource(resourcePath);
            return resource == null ? null : resource.toExternalForm();
        } catch (Exception ignored) {
            return null;
        }
    }
}
