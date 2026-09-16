package ui.pages;

import app.AppContext;
import localization.Localization;
import models.Models.KnowledgeItem;
import ui.components.Card;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public final class KnowledgePage {
    private final AppContext ctx;
    private final BorderPane root = new BorderPane();
    private final ListView<KnowledgeItem> list = new ListView<>();
    private final TextField search = new TextField();
    private final VBox detail = new VBox(8);

    public KnowledgePage(AppContext ctx) {
        this.ctx = ctx;
        root.setPadding(new Insets(20));
        root.getStyleClass().add("page");

        search.setPromptText(Localization.t("search.knowledge"));
        search.textProperty().addListener((o, a, b) -> reload());

        VBox left = new VBox(10, search, list);
        left.setPrefWidth(420);
        VBox.setVgrow(list, Priority.ALWAYS);

        detail.setPadding(new Insets(0, 20, 0, 20));

        root.setLeft(left);
        root.setCenter(detail);
        BorderPane.setMargin(detail, new Insets(0, 0, 0, 16));

        list.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> showDetail(b));
        list.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(KnowledgeItem k, boolean empty) {
                super.updateItem(k, empty);
                if (empty || k == null) { setText(null); return; }
                setText(k.title() + "   ·  conf " + String.format("%.2f", k.confidence()));
            }
        });

        ctx.activeTopic.addListener((o, a, b) -> reload());
        reload();
    }

    private void reload() {
        try {
            var t = ctx.activeTopic.get();
            if (t == null) return;
            List<KnowledgeItem> items = ctx.db.listKnowledge(t.id(), search.getText());
            list.setItems(FXCollections.observableArrayList(items));
            if (items.isEmpty()) {
                detail.getChildren().setAll(new Label(Localization.t("empty.no_knowledge")));
            }
        } catch (Exception e) { /* ignore */ }
    }

    private void showDetail(KnowledgeItem k) {
        if (k == null) return;
        Label title = new Label(k.title());
        title.getStyleClass().add("detail-title");
        Label meta = new Label("Confidence " + String.format("%.2f", k.confidence()) + " · " + k.status());
        meta.getStyleClass().add("detail-meta");
        TextArea summary = new TextArea(k.summary());
        summary.setWrapText(true); summary.setEditable(false); summary.setPrefRowCount(3);
        TextArea body = new TextArea(k.body());
        body.setWrapText(true); body.setEditable(false); body.setPrefRowCount(14);

        Button verify = new Button(Localization.t("action.verify"));
        verify.getStyleClass().add("btn-primary");
        verify.setOnAction(e -> { try { ctx.db.setKnowledgeStatus(k.id(), "VERIFIED"); reload(); } catch (Exception ex) {} });

        Button delete = new Button(Localization.t("action.delete"));
        delete.getStyleClass().add("btn-danger");
        delete.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, Localization.t("confirm.delete_knowledge"));
            a.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    try { ctx.db.deleteKnowledge(k.id()); reload(); } catch (Exception ex) {}
                }
            });
        });

        HBox actions = new HBox(8, verify, delete);
        detail.getChildren().setAll(title, meta, new Label(Localization.t("label.summary")), summary,
                new Label(Localization.t("label.body")), body, actions);
        VBox.setVgrow(body, Priority.ALWAYS);
    }

    public Node node() { return root; }
}