package ui;

import app.AppContext;
import localization.Localization;
import models.Models.Topic;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * First-run overlay. Shown when ~/.aicantsl/settings.json doesn't exist.
 * All actions are real: creates a topic, navigates to Settings, or closes.
 */
public final class WelcomeOverlay {
    private WelcomeOverlay() {}

    public static boolean isFirstRun() {
        return !Files.exists(Path.of(System.getProperty("user.home"), ".aicantsl", "settings.json"));
    }

    public static void show(Stage stage, AppContext ctx, StackPane host,
                            Runnable goToSettings, Runnable goToDashboard,
                            Consumer<String> toast) {

        StackPane scrim = new StackPane();
        scrim.setStyle("-fx-background-color: rgba(5,8,16,0.75);");

        Label brand = new Label("AI CAN'T STOP LEARNING");
        brand.getStyleClass().add("hero-title");
        brand.setStyle("-fx-font-size: 34; -fx-letter-spacing: 2;");

        Label tagline = new Label(Localization.t("welcome.tagline"));
        tagline.getStyleClass().add("hero-sub");
        tagline.setTextAlignment(TextAlignment.CENTER);
        tagline.setWrapText(true);
        tagline.setMaxWidth(560);

        TextField topicField = new TextField();
        topicField.setPromptText(Localization.t("welcome.topic_placeholder"));
        topicField.setMaxWidth(420);

        Button start = new Button(Localization.t("welcome.start"));
        start.getStyleClass().add("btn-primary");

        Button configure = new Button(Localization.t("welcome.configure"));
        configure.getStyleClass().add("btn");

        Button skip = new Button(Localization.t("welcome.skip"));
        skip.getStyleClass().add("btn-ghost");

        VBox card = new VBox(16, brand, tagline, new Separator(), topicField, start, configure, skip);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40, 60, 40, 60));
        card.getStyleClass().add("card");
        card.setMaxWidth(680);

        scrim.getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);

        // Entrance animation
        card.setOpacity(0);
        card.setTranslateY(30);
        FadeTransition ft = new FadeTransition(Duration.millis(420), card);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(420), card);
        tt.setFromY(30); tt.setToY(0);
        new ParallelTransition(ft, tt).play();

        Runnable close = () -> {
            FadeTransition out = new FadeTransition(Duration.millis(220), scrim);
            out.setFromValue(1); out.setToValue(0);
            out.setOnFinished(e -> host.getChildren().remove(scrim));
            out.play();
        };

        start.setOnAction(e -> {
            String name = topicField.getText() == null ? "" : topicField.getText().trim();
            if (name.isEmpty()) {
                toast.accept(Localization.t("welcome.need_topic"));
                return;
            }
            try {
                Topic t = ctx.db.createTopic(name, "");
                ctx.activeTopic.set(t);
                ctx.settings.put("ui.topicId", (int) t.id());
                ctx.settings.save();
            } catch (Exception ex) {
                toast.accept(Localization.t("welcome.create_failed"));
            }
            close.run();
            if (ctx.settings.aiKey().isBlank()) {
                goToSettings.run();
                toast.accept(Localization.t("welcome.configure_ai_hint"));
            } else {
                goToDashboard.run();
            }
        });

        configure.setOnAction(e -> { close.run(); goToSettings.run(); });
        skip.setOnAction(e -> { close.run(); goToDashboard.run(); });

        host.getChildren().add(scrim);
        topicField.requestFocus();
    }

    /** Convenience: no-op Node for when nothing is present. */
    public static Node empty() { return new Region(); }
}