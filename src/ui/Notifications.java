package ui;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Deque;

/** Lightweight slide-in notification stack. */
public final class Notifications {
    private final Window owner;
    private final VBox stack = new VBox(8);
    private final Deque<Popup> active = new ArrayDeque<>();

    public Notifications(Window owner) {
        this.owner = owner;
        stack.setMouseTransparent(true);
    }

    public void show(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("toast-text");
        label.setWrapText(true);
        HBox card = new HBox(label);
        card.getStyleClass().add("toast");
        card.setPadding(new Insets(10, 16, 10, 16));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(360);

        Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.getContent().add(card);
        popup.setHideOnEscape(false);

        double x = owner.getX() + owner.getWidth() - 400;
        double y = owner.getY() + 80 + active.size() * 60;
        popup.show(owner, x, y);
        active.add(popup);

        FadeTransition ft = new FadeTransition(Duration.millis(300), card);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), card);
        tt.setFromX(40); tt.setToX(0);
        new ParallelTransition(ft, tt).play();

        PauseTransition pt = new PauseTransition(Duration.seconds(4.5));
        pt.setOnFinished(e -> {
            FadeTransition out = new FadeTransition(Duration.millis(300), card);
            out.setFromValue(1); out.setToValue(0);
            out.setOnFinished(ee -> { popup.hide(); active.remove(popup); });
            out.play();
        });
        pt.play();
    }
}