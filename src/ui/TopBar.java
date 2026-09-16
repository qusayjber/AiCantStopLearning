package ui;

import app.AppContext;
import localization.Localization;
import models.Models.Topic;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * The top toolbar. Hosts the app logo + title, current topic, learning
 * controls, the topic chooser, and theme/language toggles.
 *
 * <p>The logo is loaded from {@code /icons/logo.png}. If the resource is
 * missing, the title is shown alone — the toolbar never fails to render.
 */
public final class TopBar {

    private final HBox root = new HBox(10);
    private final HBox brandBox = new HBox(10);
    private final Label title = new Label("AI CAN'T STOP LEARNING");
    private final Label topicLabel = new Label();

    private final Button startBtn = new Button();
    private final Button pauseBtn = new Button();
    private final Button stopBtn = new Button();
    private final Button topicBtn = new Button();
    private final Button langBtn = new Button();
    private final Button themeBtn = new Button();
    private final Button paletteBtn = new Button("Ctrl+K");

    private AppContext ctx;
    private Runnable onLang, onTheme, onPalette, onTopics;

    public TopBar() {
        root.getStyleClass().add("topbar");
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(12, 18, 12, 18));

        title.getStyleClass().add("topbar-title");
        topicLabel.getStyleClass().add("topbar-topic");

        // ---- Brand: logo + title ----
        brandBox.setAlignment(Pos.CENTER_LEFT);
        ImageView logo = buildLogo();
        if (logo != null) {
            brandBox.getChildren().add(logo);
        }
        brandBox.getChildren().add(title);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        startBtn.getStyleClass().addAll("btn", "btn-primary");
        pauseBtn.getStyleClass().add("btn");
        stopBtn.getStyleClass().add("btn");
        topicBtn.getStyleClass().add("btn");
        langBtn.getStyleClass().add("btn-ghost");
        themeBtn.getStyleClass().add("btn-ghost");
        paletteBtn.getStyleClass().add("btn-ghost");

        root.getChildren().addAll(
                brandBox, topicLabel, spacer,
                topicBtn, startBtn, pauseBtn, stopBtn,
                paletteBtn, langBtn, themeBtn);
    }

    /**
     * Loads the app logo from {@code /icons/logo.png}.
     * Returns null silently if the resource is missing — the toolbar
     * renders fine without it.
     */
    private static ImageView buildLogo() {
        try {
            var url = TopBar.class.getResource("/icons/logo.png");
            if (url == null) return null;

            ImageView iv = new ImageView(new Image(url.toExternalForm()));
            iv.setFitWidth(24);
            iv.setFitHeight(24);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            return iv;
        } catch (Exception e) {
            return null;
        }
    }

    public void bind(AppContext ctx,
                     Runnable onLang,
                     Runnable onTheme,
                     Runnable onPalette,
                     Runnable onTopics) {
        this.ctx = ctx;
        this.onLang = onLang;
        this.onTheme = onTheme;
        this.onPalette = onPalette;
        this.onTopics = onTopics;

        startBtn.setOnAction(e -> {
            var eng = ctx.engine.get();
            Topic t = ctx.activeTopic.get();
            if (eng != null && t != null) eng.start(t, m -> {});
        });
        pauseBtn.setOnAction(e -> { if (ctx.engine.get() != null) ctx.engine.get().pause(); });
        stopBtn.setOnAction(e -> { if (ctx.engine.get() != null) ctx.engine.get().stop(); });
        topicBtn.setOnAction(e -> { if (onTopics != null) onTopics.run(); });
        langBtn.setOnAction(e -> onLang.run());
        themeBtn.setOnAction(e -> onTheme.run());
        paletteBtn.setOnAction(e -> onPalette.run());

        ctx.activeTopic.addListener((o, a, b) -> updateTopic(b));
        updateTopic(ctx.activeTopic.get());
        applyLanguage();
    }

    private void updateTopic(Topic t) {
        topicLabel.setText(t == null ? "" : "• " + t.name());
    }

    public void applyLanguage() {
        startBtn.setText(Localization.t("action.start"));
        pauseBtn.setText(Localization.t("action.pause"));
        stopBtn.setText(Localization.t("action.stop"));
        topicBtn.setText(Localization.t("topbar.topic_button"));
        langBtn.setText(Localization.t("action.lang"));
        themeBtn.setText(Localization.t("action.theme"));
    }

    public Node node() { return root; }
}