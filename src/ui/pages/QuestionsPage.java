package ui.pages;

import app.AppContext;
import localization.Localization;
import models.Models.ResearchQuestion;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class QuestionsPage {
    private final AppContext ctx;
    private final BorderPane root = new BorderPane();
    private final ListView<ResearchQuestion> list = new ListView<>();
    private final VBox detail = new VBox(10);

    public QuestionsPage(AppContext ctx) {
        this.ctx = ctx;
        root.setPadding(new Insets(20));
        root.getStyleClass().add("page");

        list.setPrefWidth(440);
        list.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(ResearchQuestion q, boolean empty) {
                super.updateItem(q, empty);
                if (empty || q == null) { setText(null); return; }
                setText("[" + q.priority() + "] " + q.text() + "\n   " + q.status());
            }
        });
        list.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> show(b));

        root.setLeft(list);
        root.setCenter(detail);
        BorderPane.setMargin(detail, new Insets(0, 0, 0, 16));

        ctx.activeTopic.addListener((o, a, b) -> reload());
        reload();
    }

    private void reload() {
        try {
            var t = ctx.activeTopic.get();
            if (t == null) return;
            list.setItems(FXCollections.observableArrayList(ctx.db.listQuestions(t.id(), null)));
        } catch (Exception e) {}
    }

    private void show(ResearchQuestion q) {
        if (q == null) return;
        Label title = new Label(q.text());
        title.getStyleClass().add("detail-title");
        title.setWrapText(true);
        Label meta = new Label(q.priority() + " · " + q.status());
        meta.getStyleClass().add("detail-meta");
        Label answerHdr = new Label(Localization.t("label.answer"));
        TextArea answer = new TextArea(q.answer() == null ? "" : q.answer());
        answer.setWrapText(true); answer.setEditable(false); answer.setPrefRowCount(12);

        Button research = new Button(Localization.t("action.research_now"));
        research.getStyleClass().add("btn-primary");
        research.setOnAction(e -> {
            try { ctx.db.setQuestionStatus(q.id(), "OPEN"); } catch (Exception ex) {}
            var eng = ctx.engine.get();
            if (eng != null && ctx.activeTopic.get() != null) eng.start(ctx.activeTopic.get(), s -> {});
        });

        detail.getChildren().setAll(title, meta, research, answerHdr, answer);
        VBox.setVgrow(answer, Priority.ALWAYS);
    }

    public Node node() { return root; }
}