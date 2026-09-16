package app;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import ui.MainWindow;

/**
 * Launcher entry point.
 *
 * <p>Under JPMS, JavaFX's LauncherImpl refuses to launch when the main class
 * extends {@link Application} directly. Having a non-Application launcher
 * class sidesteps that restriction.
 */
public final class Launcher extends Application {

    @Override
    public void start(Stage stage) {
        // Application window icon (shown in taskbar, alt-tab, etc.)
        try {
            var iconUrl = Launcher.class.getResource("/icons/logo.png");
            if (iconUrl != null) {
                stage.getIcons().add(new Image(iconUrl.toExternalForm()));
            }
        } catch (Exception e) {
            // Icon is cosmetic — never fail startup because of it
        }

        new MainWindow(stage).show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}