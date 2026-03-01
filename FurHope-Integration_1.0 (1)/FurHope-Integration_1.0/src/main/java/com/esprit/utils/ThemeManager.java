package com.esprit.utils;

import javafx.scene.Parent;
import javafx.scene.Scene;

public final class ThemeManager {

    private static final String DARK_MODE_CLASS = "dark-mode";
    private static boolean darkModeEnabled = false;

    private ThemeManager() {
    }

    public static void applyToScene(Scene scene) {
        if (scene == null) {
            return;
        }
        Parent root = scene.getRoot();
        if (root == null) {
            return;
        }

        if (darkModeEnabled) {
            if (!root.getStyleClass().contains(DARK_MODE_CLASS)) {
                root.getStyleClass().add(DARK_MODE_CLASS);
            }
        } else {
            root.getStyleClass().remove(DARK_MODE_CLASS);
        }
    }

    public static void toggle(Scene scene) {
        darkModeEnabled = !darkModeEnabled;
        applyToScene(scene);
    }

    public static boolean isDarkModeEnabled() {
        return darkModeEnabled;
    }
}
