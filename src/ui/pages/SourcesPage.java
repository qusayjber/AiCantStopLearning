package ui.pages;

import app.AppContext;
import localization.Localization;
import models.Models.Source;
import ui.SourceCompareDialog;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.util.List;

public final class SourcesPage {
    private final AppContext ctx;
    private final BorderPane root = new BorderPane();
    private final TableView<Source> table = new TableView<>();
    private final javafx.stage.Window owner;

    public SourcesPage(AppContext ctx) {
        this.ctx = ctx;
        this.owner = null; // set lazily
        root.setPadding(new Insets(20));
        root.getStyleClass().add("page");

        TableColumn<Source, String> title = new TableColumn<>(Localization.t("col.title"));
        title.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().title()));
        title.setPrefWidth(320);

        TableColumn<Source, String> status = new TableColumn<>(Localization.t("col.status"));
        status.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().status()));
        status.setPrefWidth(110);

        TableColumn<Source, Number> cred = new TableColumn<>(Localization.t("col.credibility"));
        cred.setCellValueFactory(c -> new javafx.beans.property.SimpleDoubleProperty(c.getValue().credibility()));
        cred.setPrefWidth(110);

        TableColumn<Source, Void> actions = new TableColumn<>("");
        actions.setPrefWidth(140);
        actions.setCellFactory(col -> new TableCell<>() {
            final Button compare = new Button(Localization.t("action.compare"));
            { compare.getStyleClass().add("btn");
              compare.setOnAction(e -> {
                  Source self = getTableView().getItems().get(getIndex());
                  pickCompareTarget(self);
              });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : new HBox(compare) {{ setAlignment(Pos.CENTER); }});
            }
        });

        table.getColumns().addAll(title, status, cred, actions);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setRowFactory(tv -> {
            TableRow<Source> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    try { java.awt.Desktop.getDesktop().browse(java.net.URI.create(row.getItem().url())); }
                    catch (Exception ex) { /* ignore */ }
                }
            });
            return row;
        });

        root.setCenter(table);
        ctx.activeTopic.addListener((o, a, b) -> reload());
        reload();
    }

    private void pickCompareTarget(Source self) {
        try {
            List<Source> candidates = ctx.db.listSources(ctx.activeTopic.get().id()).stream()
                    .filter(s -> s.id() != self.id())
                    .filter(s -> s.content() != null && !s.content().isBlank())
                    .toList();
            if (candidates.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, Localization.t("compare.need_two")).showAndWait();
                return;
            }
            ChoiceDialog<Source> dlg = new ChoiceDialog<>(candidates.get(0), candidates);
            dlg.setTitle(Localization.t("compare.title"));
            dlg.setHeaderText(Localization.t("compare.pick"));
            dlg.setContentText(Localization.t("compare.pick_hint"));
            var chosen = dlg.showAndWait();
            if (chosen.isEmpty()) return;
            SourceCompareDialog.open(table.getScene().getWindow(), ctx, self, chosen.get());
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void reload() {
        try {
            var t = ctx.activeTopic.get();
            if (t == null) return;
            table.setItems(FXCollections.observableArrayList(ctx.db.listSources(t.id())));
        } catch (Exception e) { /* ignore */ }
    }

    public Node node() { return root; }
}