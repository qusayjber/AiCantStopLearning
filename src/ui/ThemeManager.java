package ui;

import javafx.scene.Scene;
import settings.AppSettings;

public final class ThemeManager {
    private ThemeManager() {}
    public static void apply(Scene scene, String theme) {
        scene.getStylesheets().clear();
        String path = "dark".equals(theme) ? "/styles/dark.css" : "/styles/light.css";
        var url = ThemeManager.class.getResource(path);
        if (url != null) scene.getStylesheets().add(url.toExternalForm());
    }
    public static void toggle(AppSettings s, Scene scene) {
        String next = "dark".equals(s.theme()) ? "light" : "dark";
        s.put("ui.theme", next);
        s.save();
        apply(scene, next);
    }
}