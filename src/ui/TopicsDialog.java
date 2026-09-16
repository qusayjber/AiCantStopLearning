package ui;

import app.AppContext;
import localization.Localization;
import models.Models.Topic;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;

/**
 * Modal dialog for choosing or creating a research topic.
 *
 * <p>Lists every topic in the database, allows selection (double-click or
 * "Open"), and offers a "New Topic" flow that creates a row in SQLite and
 * switches the active topic.
 */
public final class TopicsDialog {

    private TopicsDialog() {}

    /**
     * Opens the topic chooser.
     *
     * @param owner    the parent window
     * @param ctx      the application context
     * @param onPicked callback invoked after a topic is selected or created
     */
    public static void open(Window owner, AppContext ctx, Runnable onPicked) {
        Stage st = new Stage();
        st.initOwner(owner);
        st.initModality(Modality.WINDOW_MODAL);
        st.setTitle(Localization.t("topics.title"));
        st.setWidth(560);
        st.setHeight(520);

        // ---- Topic list ----
        ListView<Topic> list = new ListView<>();
        list.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Topic t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) {
                    setText(null);
                    return;
                }
                String desc = t.description() == null || t.description().isBlank()
                        ? "" : "  —  " + t.description();
                setText(t.name() + desc);
            }
        });

        Runnable refreshList = () -> {
            try {
                List<Topic> all = ctx.db.listTopics();
                list.setItems(FXCollections.observableArrayList(all));
                Topic active = ctx.activeTopic.get();
                if (active != null) {
                    for (Topic t : all) {
                        if (t.id() == active.id()) {
                            list.getSelectionModel().select(t);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR,
                        "Could not load topics: " + e.getMessage()).showAndWait();
            }
        };

        // ---- New topic form (hidden by default) ----
        TextField nameField = new TextField();
        nameField.setPromptText(Localization.t("topics.name_placeholder"));

        TextField descField = new TextField();
        descField.setPromptText(Localization.t("topics.desc_placeholder"));

        Button createBtn = new Button(Localization.t("topics.create"));
        createBtn.getStyleClass().add("btn-primary");

        Button cancelCreateBtn = new Button(Localization.t("action.cancel"));
        cancelCreateBtn.getStyleClass().add("btn-ghost");

        HBox createActions = new HBox(8, createBtn, cancelCreateBtn);
        createActions.setAlignment(Pos.CENTER_RIGHT);

        VBox createForm = new VBox(8,
                new Label(Localization.t("topics.new")),
                nameField,
                descField,
                createActions);
        createForm.setPadding(new Insets(12));
        createForm.getStyleClass().add("card");
        createForm.setVisible(false);
        createForm.setManaged(false);

        // ---- Action buttons ----
        Button newBtn = new Button(Localization.t("topics.new"));
        newBtn.getStyleClass().add("btn");

        Button deleteBtn = new Button(Localization.t("action.delete"));
        deleteBtn.getStyleClass().add("btn-danger");

        Button openBtn = new Button(Localization.t("action.open"));
        openBtn.getStyleClass().add("btn-primary");

        Button closeBtn = new Button(Localization.t("action.cancel"));
        closeBtn.getStyleClass().add("btn-ghost");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bottomBar = new HBox(8, newBtn, deleteBtn, spacer, closeBtn, openBtn);
        bottomBar.setAlignment(Pos.CENTER_LEFT);

        // ---- Layout ----
        VBox root = new VBox(12, list, createForm, bottomBar);
        root.setPadding(new Insets(16));
        root.getStyleClass().add("page");
        VBox.setVgrow(list, Priority.ALWAYS);

        // ---- Wiring ----
        Runnable pickSelected = () -> {
            Topic t = list.getSelectionModel().getSelectedItem();
            if (t == null) return;
            ctx.activeTopic.set(t);
            ctx.settings.put("ui.topicId", (int) t.id());
            ctx.settings.save();
            st.close();
            onPicked.run();
        };

        openBtn.setOnAction(e -> pickSelected.run());
        list.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) pickSelected.run();
        });

        newBtn.setOnAction(e -> {
            createForm.setVisible(true);
            createForm.setManaged(true);
            nameField.requestFocus();
        });

        cancelCreateBtn.setOnAction(e -> {
            createForm.setVisible(false);
            createForm.setManaged(false);
            nameField.clear();
            descField.clear();
        });

        createBtn.setOnAction(e -> {
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            String desc = descField.getText() == null ? "" : descField.getText().trim();
            if (name.isEmpty()) {
                new Alert(Alert.AlertType.WARNING,
                        Localization.t("topics.need_name")).showAndWait();
                return;
            }
            try {
                Topic created = ctx.db.createTopic(name, desc);
                if (created != null) {
                    ctx.activeTopic.set(created);
                    ctx.settings.put("ui.topicId", (int) created.id());
                    ctx.settings.save();
                }
                nameField.clear();
                descField.clear();
                createForm.setVisible(false);
                createForm.setManaged(false);
                refreshList.run();
                pickSelected.run();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR,
                        "Could not create topic: " + ex.getMessage()).showAndWait();
            }
        });

        deleteBtn.setOnAction(e -> {
            Topic t = list.getSelectionModel().getSelectedItem();
            if (t == null) return;
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    Localization.t("topics.confirm_delete") + "\n\n" + t.name());
            confirm.setHeaderText(null);
            var result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    ctx.db.deleteTopic(t.id());
                    if (ctx.activeTopic.get() != null
                            && ctx.activeTopic.get().id() == t.id()) {
                        ctx.activeTopic.set(null);
                    }
                    refreshList.run();
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "Could not delete topic: " + ex.getMessage()).showAndWait();
                }
            }
        });

        closeBtn.setOnAction(e -> st.close());

        // ---- Scene ----
        Scene scene = new Scene(root);
        var css = TopicsDialog.class.getResource(
                "dark".equals(ctx.settings.theme()) ? "/styles/dark.css" : "/styles/light.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        Localization.applyOrientation(root);
        st.setScene(scene);

        refreshList.run();
        st.showAndWait();
    }
}