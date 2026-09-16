package ui.pages;

import app.AppContext;
import localization.Localization;
import models.Models.*;
import ui.components.Card;
import ui.components.StatusDot;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Main dashboard — hero card with session state, live statistics grid,
 * and a rolling activity timeline.
 */
public final class DashboardPage {

    private final AppContext ctx;
    private final VBox root = new VBox(16);
    private final Consumer<String> toast;

    private final Label sessionTitle = new Label();
    private final Label targetLabel  = new Label();
    private final Label statusLabel  = new Label();
    private final StatusDot dot      = new StatusDot();
    private final ProgressBar progress = new ProgressBar(0);
    private final Label activityLabel = new Label();
    private final VBox statsGrid = new VBox(10);
    private final VBox timeline  = new VBox(6);

    private final javafx.animation.Timeline statsTimer;

    public DashboardPage(AppContext ctx, Consumer<String> toast) {
        this.ctx = ctx;
        this.toast = toast;

        root.setPadding(new Insets(20));
        root.getStyleClass().add("page");

        // ---------- Hero card ----------
        VBox hero = new VBox(8);

        // Brand row: logo + title
        HBox brandRow = new HBox(12);
        brandRow.setAlignment(Pos.CENTER_LEFT);

        ImageView logo = buildLogo();
        if (logo != null) brandRow.getChildren().add(logo);

        sessionTitle.getStyleClass().add("hero-title");
        brandRow.getChildren().add(sessionTitle);

        targetLabel.getStyleClass().add("hero-sub");

        HBox statusRow = new HBox(10, dot.node(), statusLabel);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusLabel.getStyleClass().add("hero-status");

        progress.setMaxWidth(Double.MAX_VALUE);
        progress.getStyleClass().add("hero-progress");
        activityLabel.getStyleClass().add("hero-activity");

        hero.getChildren().addAll(
                brandRow,
                statusRow,
                targetLabel,
                progress,
                activityLabel);
        Card heroCard = new Card(hero);

        // ---------- Stats card ----------
        VBox stats = new VBox(8);
        Label statsTitle = new Label(Localization.t("dash.stats"));
        statsTitle.getStyleClass().add("section-title");
        stats.getChildren().addAll(statsTitle, statsGrid);
        Card statsCard = new Card(stats);

        // ---------- Timeline card ----------
        VBox tl = new VBox(8);
        Label tlTitle = new Label(Localization.t("dash.activity"));
        tlTitle.getStyleClass().add("section-title");
        tl.getChildren().addAll(tlTitle, timeline);
        Card tlCard = new Card(tl);

        HBox row = new HBox(16, heroCard.node(), statsCard.node());
        HBox.setHgrow(heroCard.node(), Priority.ALWAYS);
        HBox.setHgrow(statsCard.node(), Priority.ALWAYS);

        root.getChildren().addAll(row, tlCard.node());
        VBox.setVgrow(tlCard.node(), Priority.ALWAYS);

        // ---------- Live listeners ----------
        ctx.activeTopic.addListener((o, a, b) -> refresh());

        var engine = ctx.engine.get();
        if (engine != null) {
            engine.statusProperty().addListener((o, a, b) -> refresh());
            engine.activityProperty().addListener((o, a, b) ->
                    activityLabel.setText(b == null ? "" : b));
            engine.currentTargetProperty().addListener((o, a, b) ->
                    targetLabel.setText(Localization.t("dash.target") + ": "
                            + (b == null ? "" : b)));
        }

        refresh();

        // ---------- Stats polling ----------
        statsTimer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.seconds(3), e -> refreshStats()));
        statsTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        statsTimer.play();
    }

    /**
     * Loads the app logo. Tries PNG first (JavaFX native), then SVG as a
     * fallback (won't render but keeps the code path uniform). Returns null
     * silently if neither is present — the dashboard renders fine without it.
     */
    private static ImageView buildLogo() {
        try {
            var url = DashboardPage.class.getResource("/icons/logo.png");
            if (url == null) return null;

            ImageView iv = new ImageView(new Image(url.toExternalForm()));
            iv.setFitWidth(40);
            iv.setFitHeight(40);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            return iv;
        } catch (Exception e) {
            // Cosmetic only — never fail the dashboard because of a missing asset.
            return null;
        }
    }

    private void refresh() {
        Topic t = ctx.activeTopic.get();
        if (t == null) {
            sessionTitle.setText(Localization.t("dash.no_session"));
            statusLabel.setText("");
            targetLabel.setText("");
            activityLabel.setText("");
            progress.setProgress(0);
            return;
        }

        sessionTitle.setText(t.name());

        var engine = ctx.engine.get();
        if (engine != null) {
            var s = engine.statusProperty().get();
            if (s != null) {
                statusLabel.setText(Localization.t("status." + s.name().toLowerCase()));
                dot.setStatus(s.name());
                progress.setProgress(switch (s) {
                    case RUNNING -> -1;   // indeterminate
                    case PAUSED  -> 0.5;
                    default      -> 0;
                });
            }
        }

        refreshStats();
        refreshTimeline();
    }

    private void refreshStats() {
        Topic t = ctx.activeTopic.get();
        if (t == null) return;
        try {
            Map<String, Integer> m = ctx.db.stats(t.id());
            statsGrid.getChildren().clear();

            boolean allZero = m.values().stream().allMatch(v -> v == 0);

            for (var e : m.entrySet()) {
                HBox row = new HBox(8);
                Label k = new Label(Localization.t("stat." + e.getKey()));
                k.getStyleClass().add("stat-key");
                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);
                Label v = new Label(String.valueOf(e.getValue()));
                v.getStyleClass().add("stat-val");
                row.getChildren().addAll(k, sp, v);
                statsGrid.getChildren().add(row);
            }

            if (allZero) {
                Label empty = new Label(Localization.t("empty.no_knowledge"));
                empty.getStyleClass().add("empty");
                statsGrid.getChildren().add(empty);
            }
        } catch (Exception ex) {
            /* DB errors should not crash the dashboard */
        }
    }

    private void refreshTimeline() {
        Topic t = ctx.activeTopic.get();
        if (t == null) return;
        try {
            var events = ctx.db.listEvents(t.id(), 15);
            timeline.getChildren().clear();

            if (events.isEmpty()) {
                Label empty = new Label(Localization.t("empty.no_activity"));
                empty.getStyleClass().add("empty");
                timeline.getChildren().add(empty);
                return;
            }

            for (var ev : events) {
                HBox row = new HBox(10);
                Label time = new Label(
                        ev.at().atZone(java.time.ZoneId.systemDefault())
                                .toLocalTime().withNano(0).toString());
                time.getStyleClass().add("event-time");

                Label msg = new Label(ev.message());
                msg.getStyleClass().add("event-msg");
                msg.setWrapText(true);

                row.getChildren().addAll(time, msg);
                timeline.getChildren().add(row);
            }
        } catch (Exception ex) {
            /* ignore */
        }
    }

    /** Stops the polling timer. Safe to call multiple times. */
    public void dispose() {
        if (statsTimer != null) statsTimer.stop();
    }

    /** Returns the JavaFX Node root of this page. */
    public Node node() { return root; }
}