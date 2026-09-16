package ui;

import app.AppContext;
import localization.Localization;
import models.Models.Topic;
import ui.pages.*;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public final class MainWindow {

    private final Stage stage;
    private AppContext ctx;

    private final BorderPane root = new BorderPane();
    private final StackPane content = new StackPane();
    private final Sidebar sidebar = new Sidebar();
    private final TopBar topBar = new TopBar();

    private Notifications notifications;
    private Scene scene;

    private final Map<String, Node> pageCache = new HashMap<>();
    private final Map<String, Supplier<Node>> pageFactories = new LinkedHashMap<>();

    public MainWindow(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        try {
            ctx = new AppContext();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Startup error");
            alert.setHeaderText("Failed to start AI CAN'T STOP LEARNING");
            alert.setContentText(e.getMessage() == null ? e.toString() : e.getMessage());
            alert.showAndWait();
            Platform.exit();
            return;
        }

        registerPages();
        buildScene();
        loadOrCreateActiveTopic();

        stage.setOnShown(e -> notifications = new Notifications(stage));

        sidebar.setOnSelect(this::navigate);
        sidebar.setItems(List.of(
                "dashboard", "knowledge", "sources", "questions",
                "gaps", "graph", "history", "settings"));

        navigate("dashboard");

        stage.setTitle("AI CAN'T STOP LEARNING");
        stage.show();

        // First-run welcome if no topic exists yet
        if (ctx.activeTopic.get() == null) {
            openTopicsDialog();
        }
    }

    private void registerPages() {
        pageFactories.put("dashboard", () -> new DashboardPage(ctx, this::toast).node());
        pageFactories.put("knowledge", () -> new KnowledgePage(ctx).node());
        pageFactories.put("sources",   () -> new SourcesPage(ctx).node());
        pageFactories.put("questions", () -> new QuestionsPage(ctx).node());
        pageFactories.put("gaps",      () -> new GapsPage(ctx).node());
        pageFactories.put("graph",     () -> new GraphPage(ctx).node());
        pageFactories.put("history",   () -> new HistoryPage(ctx).node());
        pageFactories.put("settings",  () -> new SettingsPage(ctx, this::onSettingsChanged).node());
    }

    private void buildScene() {
        root.setTop(topBar.node());
        root.setLeft(sidebar.node());
        root.setCenter(content);
        BorderPane.setMargin(content, new Insets(0));

        topBar.bind(ctx,
                this::toggleLanguage,
                this::toggleTheme,
                this::openPalette,
                this::openTopicsDialog);   // ← زر الموضوع في الشريط العلوي
        sidebar.applyLanguage();

        scene = new Scene(root, 1360, 820);
        ThemeManager.apply(scene, ctx.settings.theme());
        Localization.applyOrientation(root);

        stage.setScene(scene);

        Localization.localeProperty().addListener((o, a, b) -> {
            Localization.applyOrientation(root);
            sidebar.applyLanguage();
            topBar.applyLanguage();
            pageCache.clear();
            content.getChildren().clear();
            String current = sidebar.selected();
            navigate(current == null ? "dashboard" : current);
        });

        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.K, KeyCombination.CONTROL_DOWN),
                this::openPalette);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                this::toggleLanguage);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                this::toggleTheme);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN),
                this::openTopicsDialog);
    }

    private void loadOrCreateActiveTopic() {
        try {
            List<Topic> topics = ctx.db.listTopics();
            long stored = ctx.settings.topicId();
            Topic active = topics.stream()
                    .filter(t -> t.id() == stored)
                    .findFirst()
                    .orElse(topics.isEmpty() ? null : topics.get(0));

            if (active == null) {
                active = ctx.db.createTopic("Distributed Systems",
                        "Core distributed computing concepts");
            }

            ctx.activeTopic.set(active);
            ctx.settings.put("ui.topicId", (int) active.id());
            ctx.settings.save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void navigate(String key) {
        Node page = pageCache.computeIfAbsent(key, k -> {
            Supplier<Node> factory = pageFactories.get(k);
            return factory == null
                    ? new Label("Unknown page: " + k)
                    : factory.get();
        });

        content.getChildren().setAll(page);
        sidebar.select(key);

        FadeTransition ft = new FadeTransition(Duration.millis(200), page);
        ft.setFromValue(0.4);
        ft.setToValue(1.0);
        ft.play();
    }

    private void openTopicsDialog() {
        TopicsDialog.open(stage, ctx, () -> {
            pageCache.clear();
            content.getChildren().clear();
            String current = sidebar.selected();
            navigate(current == null ? "dashboard" : current);
            Topic t = ctx.activeTopic.get();
            toast(t == null
                    ? Localization.t("dash.no_session")
                    : Localization.t("topics.title") + ": " + t.name());
        });
    }

    private void toggleLanguage() {
        var current = Localization.currentLocale().getLanguage();
        var next = "ar".equals(current)
                ? Locale.forLanguageTag("en")
                : Locale.forLanguageTag("ar");
        Localization.setLocale(next);
        ctx.settings.put("ui.language", next.getLanguage());
        ctx.settings.save();
    }

    private void toggleTheme() {
        ThemeManager.toggle(ctx.settings, scene);
    }

    private void onSettingsChanged() {
        ctx.rebuildProviders();
        pageCache.clear();
        String current = sidebar.selected();
        navigate(current == null ? "dashboard" : current);
        toast(Localization.t("toast.settings_saved"));
    }

    public void toast(String msg) {
        if (notifications != null) notifications.show(msg);
    }

    private void openPalette() {
        List<CommandPalette.Command> cmds = new ArrayList<>();

        cmds.add(new CommandPalette.Command(Localization.t("cmd.topics"),     this::openTopicsDialog));
        cmds.add(new CommandPalette.Command(Localization.t("cmd.start"), () -> {
            var eng = ctx.engine.get();
            var topic = ctx.activeTopic.get();
            if (eng != null && topic != null) {
                eng.start(topic, this::toast);
            } else if (topic == null) {
                toast(Localization.t("topics.title"));
                openTopicsDialog();
            }
        }));
        cmds.add(new CommandPalette.Command(Localization.t("cmd.pause"), () -> {
            var eng = ctx.engine.get();
            if (eng != null) eng.pause();
        }));
        cmds.add(new CommandPalette.Command(Localization.t("cmd.resume"), () -> {
            var eng = ctx.engine.get();
            if (eng != null) eng.resume();
        }));
        cmds.add(new CommandPalette.Command(Localization.t("cmd.stop"), () -> {
            var eng = ctx.engine.get();
            if (eng != null) eng.stop();
        }));

        cmds.add(new CommandPalette.Command(Localization.t("cmd.dashboard"), () -> navigate("dashboard")));
        cmds.add(new CommandPalette.Command(Localization.t("cmd.knowledge"), () -> navigate("knowledge")));
        cmds.add(new CommandPalette.Command(Localization.t("cmd.sources"),   () -> navigate("sources")));
        cmds.add(new CommandPalette.Command(Localization.t("cmd.questions"), () -> navigate("questions")));
        cmds.add(new CommandPalette.Command(Localization.t("cmd.gaps"),      () -> navigate("gaps")));
        cmds.add(new CommandPalette.Command(Localization.t("cmd.graph"),     () -> navigate("graph")));
        cmds.add(new CommandPalette.Command(Localization.t("cmd.history"),   () -> navigate("history")));
        cmds.add(new CommandPalette.Command(Localization.t("cmd.settings"),  () -> navigate("settings")));

        cmds.add(new CommandPalette.Command(Localization.t("cmd.theme"),    this::toggleTheme));
        cmds.add(new CommandPalette.Command(Localization.t("cmd.language"), this::toggleLanguage));

        CommandPalette.open(stage, cmds, this::toast);
    }
}