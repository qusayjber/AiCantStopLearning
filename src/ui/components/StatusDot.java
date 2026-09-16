package ui.components;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public final class StatusDot {
    public enum Status { IDLE, RUNNING, PAUSED, ERROR }
    private final Circle outer = new Circle(8);
    private final Circle inner = new Circle(4);
    private final javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(outer, inner);
    private Timeline pulse;

    public StatusDot() {
        outer.setOpacity(0.35);
        setStatus("IDLE");
    }

    public void setStatus(String s) {
        Status st;
        try { st = Status.valueOf(s); } catch (Exception e) { st = Status.IDLE; }
        Color c = switch (st) {
            case RUNNING -> Color.web("#38d39f");
            case PAUSED  -> Color.web("#ffc857");
            case ERROR   -> Color.web("#ff5c7c");
            default       -> Color.web("#7c8aa5");
        };
        inner.setFill(c);
        outer.setFill(c);
        if (pulse != null) pulse.stop();
        if (st == Status.RUNNING) {
            pulse = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(outer.opacityProperty(), 0.2)),
                    new KeyFrame(Duration.millis(800), new KeyValue(outer.opacityProperty(), 0.9)),
                    new KeyFrame(Duration.millis(1600), new KeyValue(outer.opacityProperty(), 0.2)));
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.play();
        } else {
            outer.setOpacity(0.35);
        }
    }

    public Node node() { return root; }
}