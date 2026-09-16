package ui.pages;

import app.AppContext;
import localization.Localization;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class HistoryPage {
    private final AppContext ctx;
    private final VBox root = new VBox(8);
    private final VBox timeline = new VBox(6);

    public HistoryPage(AppContext ctx) {
        this.ctx = ctx;
        root.setPadding(new Insets(20));
        root.getStyleClass().add("page");

        ScrollPane sp = new ScrollPane(timeline);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("history-scroll");

        Button refresh = new Button(Localization.t("action.reload"));
        refresh.getStyleClass().add("btn");
        refresh.setOnAction(e -> reload());

        root.getChildren().addAll(refresh, sp);
        VBox.setVgrow(sp, Priority.ALWAYS);

        ctx.activeTopic.addListener((o, a, b) -> reload());
        reload();
    }

    private void reload() {
        timeline.getChildren().clear();
        try {
            var t = ctx.activeTopic.get();
            if (t == null) return;
            var events = ctx.db.listEvents(t.id(), 200);
            if (events.isEmpty()) {
                Label empty = new Label(Localization.t("empty.no_activity"));
                empty.getStyleClass().add("empty");
                timeline.getChildren().add(empty);
                return;
            }
            for (var ev : events) {
                HBox row = new HBox(12);
                Label time = new Label(ev.at().atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime().toString().replace('T', ' '));
                time.getStyleClass().add("event-time");
                Label kind = new Label("[" + ev.kind() + "]");
                kind.getStyleClass().add("event-kind");
                Label msg = new Label(ev.message());
                msg.getStyleClass().add("event-msg");
                msg.setWrapText(true);
                row.getChildren().addAll(time, kind, msg);
                timeline.getChildren().add(row);
            }
        } catch (Exception e) { /* ignore */ }
    }

    public Node node() { return root; }
}