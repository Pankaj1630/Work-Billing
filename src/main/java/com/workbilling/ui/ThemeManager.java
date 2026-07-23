package com.workbilling.ui;

import com.workbilling.service.SettingsService;
import javafx.scene.Scene;

public final class ThemeManager {
    private static final String DARK_MODE_KEY = "dark_mode";
    private static final String DARK_THEME_CLASS = "dark-theme";

    private final SettingsService settingsService = new SettingsService();
    private Scene scene;

    public void bindScene(Scene scene) {
        this.scene = scene;
    }

    public boolean isDarkMode() {
        try {
            return "true".equalsIgnoreCase(settingsService.getSetting(DARK_MODE_KEY));
        } catch (Exception e) {
            return false;
        }
    }

    public void applySavedTheme() {
        applyTheme(isDarkMode());
    }

    public void applyTheme(boolean darkMode) {
        if (scene == null) {
            return;
        }
        if (darkMode) {
            if (!scene.getRoot().getStyleClass().contains(DARK_THEME_CLASS)) {
                scene.getRoot().getStyleClass().add(DARK_THEME_CLASS);
            }
        } else {
            scene.getRoot().getStyleClass().remove(DARK_THEME_CLASS);
        }
        try {
            settingsService.setSetting(DARK_MODE_KEY, String.valueOf(darkMode));
        } catch (Exception ignored) {
        }
    }
}
