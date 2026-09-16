package ui.components;

import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

public final class Card {
    private final VBox box = new VBox();
    public Card(Node content) {
        box.getStyleClass().add("card");
        box.setPadding(new Insets(18));
        box.getChildren().add(content);
    }
    public Node node() { return box; }
}