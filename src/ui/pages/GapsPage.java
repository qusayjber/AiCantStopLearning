package ui.pages;

import app.AppContext;
import localization.Localization;
import models.Models.KnowledgeGap;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Knowledge gaps page.
 *
 * <p>Lists the gaps the learning engine has detected, ordered by priority
 * (HIGH → MEDIUM → LOW). Selecting a gap shows its reason, the associated
 * research state, and offers a "Learn this now" action that pushes the gap
 * back to the front of the research queue.
 */
public final class GapsPage {

    private final AppContext ctx;
    private final BorderPane root = new BorderPane();
    private final ListView<KnowledgeGap> list = new ListView<>();
    private final VBox detail = new VBox(10);

    public GapsPage(AppContext ctx) {
        this.ctx = ctx;
        root.setPadding(new Insets(20));
        root.getStyleClass().add("page");

        // ---- Left: list of gaps ----
        list.setPrefWidth(440);
        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(KnowledgeGap g, boolean empty) {
                super.updateItem(g, empty);
                if (empty || g == null) {
                    setText(null);
                    return;
                }
                setText("[" + g.priority() + "] " + g.description());
            }
        });
        list.getSelectionModel().selectedItemProperty()
                .addListener((o, a, b) -> show(b));

        // ---- Right: detail pane ----
        detail.setPadding(new Insets(0, 0, 0, 16));

        root.setLeft(list);
        root.setCenter(detail);

        // Reload whenever the active topic changes.
        ctx.activeTopic.addListener((o, a, b) -> reload());
        reload();
    }

    /** Refreshes the list from the database. */
    private void reload() {
        try {
            var t = ctx.activeTopic.get();
            if (t == null) return;
            list.setItems(FXCollections.observableArrayList(ctx.db.listGaps(t.id())));
            if (list.getItems().isEmpty()) {
                detail.getChildren().setAll(
                        new Label(Localization.t("empty.no_gaps")));
            }
        } catch (Exception e) {
            /* ignore — DB errors should not crash the page */
        }
    }

    /** Renders the detail pane for the selected gap. */
    private void show(KnowledgeGap g) {
        if (g == null) return;

        Label title = new Label(g.description());
        title.getStyleClass().add("detail-title");
        title.setWrapText(true);

        Label meta = new Label(g.priority() + " · " + g.status());
        meta.getStyleClass().add("detail-meta");

        Label reasonHeader = new Label(Localization.t("label.reason"));
        reasonHeader.getStyleClass().add("settings-label");

        Label reason = new Label(g.reason() == null ? "" : g.reason());
        reason.setWrapText(true);
        reason.getStyleClass().add("detail-meta");

        Button learnNow = new Button(Localization.t("action.learn_now"));
        learnNow.getStyleClass().add("btn-primary");
        learnNow.setOnAction(e -> {
            // Mark the gap as being investigated, then ask the engine to start.
            try {
                ctx.db.setGapStatus(g.id(), "INVESTIGATING");
            } catch (Exception ignored) { /* best effort */ }

            var eng = ctx.engine.get();
            var topic = ctx.activeTopic.get();
            if (eng != null && topic != null) {
                eng.start(topic, s -> { /* toast shown by MainWindow */ });
            }
        });

        detail.getChildren().setAll(
                title,
                meta,
                new Separator(),
                reasonHeader,
                reason,
                learnNow);
    }

    /** Returns the JavaFX Node root of this page. */
    public Node node() { return root; }
}