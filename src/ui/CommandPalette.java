package ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.util.*;
import java.util.function.Consumer;

/** Ctrl+K command palette. */
public final class CommandPalette {
    public record Command(String title, Runnable action) {}

    public static void open(Window owner, List<Command> commands, Consumer<String> notifier) {
        Stage st = new Stage(StageStyle.TRANSPARENT);
        st.initOwner(owner);

        TextField input = new TextField();
        input.setPromptText("Type a command…");
        input.getStyleClass().add("palette-input");
        ListView<Command> list = new ListView<>();
        list.getStyleClass().add("palette-list");
        list.setPrefHeight(320);

        Runnable refresh = () -> {
            String q = input.getText() == null ? "" : input.getText().toLowerCase();
            List<Command> filtered = new ArrayList<>();
            for (Command c : commands) if (c.title().toLowerCase().contains(q)) filtered.add(c);
            list.setItems(FXCollections.observableArrayList(filtered));
            if (!filtered.isEmpty()) list.getSelectionModel().select(0);
        };
        refresh.run();
        input.textProperty().addListener((o, a, b) -> refresh.run());

        Runnable run = () -> {
            Command c = list.getSelectionModel().getSelectedItem();
            if (c != null) { st.close(); c.action().run(); }
        };
        input.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) { list.getSelectionModel().selectNext(); e.consume(); }
            else if (e.getCode() == KeyCode.UP) { list.getSelectionModel().selectPrevious(); e.consume(); }
            else if (e.getCode() == KeyCode.ENTER) run.run();
            else if (e.getCode() == KeyCode.ESCAPE) st.close();
        });
        list.setOnMouseClicked(e -> { if (e.getClickCount() == 1) run.run(); });

        VBox box = new VBox(6, input, list);
        box.getStyleClass().add("palette");
        box.setPadding(new Insets(12));
        box.setPrefWidth(560);

        Scene scene = new Scene(box);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        var css = CommandPalette.class.getResource("/styles/dark.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        st.setScene(scene);
        st.setWidth(560); st.setHeight(400);
        st.setX(owner.getX() + (owner.getWidth() - 560) / 2);
        st.setY(owner.getY() + 120);
        st.show();
        input.requestFocus();
    }
}