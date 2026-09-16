package ui;

import localization.Localization;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.util.*;
import java.util.function.Consumer;

public final class Sidebar {
    private final VBox root = new VBox(4);
    private final Map<String, HBox> items = new LinkedHashMap<>();
    private final Map<String, Label> labels = new LinkedHashMap<>();
    private Consumer<String> onSelect = k -> {};
    private String selected = "dashboard";

    public Sidebar() {
        root.getStyleClass().add("sidebar");
        root.setPrefWidth(220);
        root.setPadding(new Insets(20, 12, 20, 12));

        Label brand = new Label("AI CAN'T STOP\nLEARNING");
        brand.getStyleClass().add("brand");
        brand.setWrapText(true);
        root.getChildren().add(brand);
        root.getChildren().add(new Region() {{ setPrefHeight(20); }});
    }

    public void setOnSelect(Consumer<String> c) { this.onSelect = c; }

    public void setItems(List<String> keys) {
        for (String k : keys) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 12, 10, 12));
            row.getStyleClass().add("nav-item");

            Circle dot = new Circle(4);
            dot.getStyleClass().add("nav-dot");

            Label lbl = new Label();
            lbl.getStyleClass().add("nav-label");

            row.getChildren().addAll(dot, lbl);
            row.setOnMouseClicked(e -> onSelect.accept(k));
            row.setOnMouseEntered(e -> row.getStyleClass().add("nav-item-hover"));
            row.setOnMouseExited(e -> row.getStyleClass().remove("nav-item-hover"));

            items.put(k, row);
            labels.put(k, lbl);
            root.getChildren().add(row);
        }
        applyLanguage();
        select(selected);
    }

    public void applyLanguage() {
        for (var e : labels.entrySet())
            e.getValue().setText(Localization.t("nav." + e.getKey()));
    }

    public void select(String key) {
        selected = key;
        for (var e : items.entrySet()) {
            e.getValue().getStyleClass().remove("nav-item-active");
            if (e.getKey().equals(key)) e.getValue().getStyleClass().add("nav-item-active");
        }
    }

    public String selected() { return selected; }
    public Node node() { return root; }
}