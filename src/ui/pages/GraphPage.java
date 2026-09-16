package ui.pages;

import app.AppContext;
import localization.Localization;
import models.Models.*;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.*;

/**
 * Force-directed concept graph rendered on a Canvas.
 *
 * <p>
 * Uses an internal class {@code GraphNode} — NOT {@code Node} — to avoid
 * clashing with {@link javafx.scene.Node}.
 */
public final class GraphPage {

	private final AppContext ctx; // ← لا تُهيّئها هنا، استقبلها من المُنشئ
	private final BorderPane root = new BorderPane();
	private final Pane canvasHolder = new Pane();
	private final Canvas canvas = new Canvas(1000, 700);

	private final List<GraphNode> nodes = new ArrayList<>();
	private final List<Edge> edges = new ArrayList<>();
	private double scale = 1.0;
	private double offsetX = 0, offsetY = 0;
	private double lastX, lastY;

	/** Internal graph vertex. Renamed to avoid shadowing javafx.scene.Node. */
	private static final class GraphNode {
		long id;
		String label;
		double x, y, vx, vy;
		int mentions;

		GraphNode(long id, String label, int m) {
			this.id = id;
			this.label = label;
			this.mentions = m;
			this.x = Math.random() * 800;
			this.y = Math.random() * 600;
		}
	}

	private record Edge(GraphNode a, GraphNode b) {
	}

	public GraphPage(AppContext ctx) { // ← GraphPage، وليس GapsPage
		this.ctx = ctx;
		root.setPadding(new Insets(20));
		root.getStyleClass().add("page");

		canvasHolder.getChildren().add(canvas);
		canvas.widthProperty().bind(canvasHolder.widthProperty());
		canvas.heightProperty().bind(canvasHolder.heightProperty());

		Button reload = new Button(Localization.t("action.reload"));
		reload.getStyleClass().add("btn");
		reload.setOnAction(e -> reload());

		Button fit = new Button(Localization.t("action.reset_view"));
		fit.getStyleClass().add("btn");
		fit.setOnAction(e -> {
			scale = 1.0;
			offsetX = offsetY = 0;
		});

		HBox toolbar = new HBox(8, reload, fit);
		toolbar.setPadding(new Insets(0, 0, 12, 0));
		toolbar.setAlignment(Pos.CENTER_LEFT);

		root.setTop(toolbar);
		root.setCenter(canvasHolder);
		VBox.setVgrow(canvasHolder, Priority.ALWAYS);

		canvasHolder.setOnScroll((ScrollEvent e) -> {
			scale *= (e.getDeltaY() > 0) ? 1.1 : 0.9;
			scale = Math.max(0.2, Math.min(4, scale));
		});
		canvasHolder.setOnMousePressed((MouseEvent e) -> {
			lastX = e.getX();
			lastY = e.getY();
		});
		canvasHolder.setOnMouseDragged((MouseEvent e) -> {
			offsetX += e.getX() - lastX;
			offsetY += e.getY() - lastY;
			lastX = e.getX();
			lastY = e.getY();
		});
		canvasHolder.setOnMouseClicked(e -> {
			for (GraphNode n : nodes) {
				double dx = (n.x - (e.getX() - offsetX) / scale);
				double dy = (n.y - (e.getY() - offsetY) / scale);
				if (dx * dx + dy * dy < 200) {
					new Alert(Alert.AlertType.INFORMATION, n.label + " (" + n.mentions + " mentions)").showAndWait();
					return;
				}
			}
		});

		ctx.activeTopic.addListener((o, a, b) -> reload());
		reload();

		AnimationTimer timer = new AnimationTimer() {
			@Override
			public void handle(long now) {
				step();
				draw();
			}
		};
		timer.start();
	}

	private void reload() {
		nodes.clear();
		edges.clear();
		Topic t = ctx.activeTopic.get();
		if (t == null)
			return;
		try {
			List<Concept> concepts = ctx.db.listConcepts(t.id());
			Map<Long, GraphNode> byId = new HashMap<>();
			for (Concept c : concepts) {
				GraphNode n = new GraphNode(c.id(), c.name(), c.mentions());
				nodes.add(n);
				byId.put(c.id(), n);
			}
			for (Relationship r : ctx.db.listRelationships(t.id())) {
				GraphNode a = byId.get(r.fromConceptId());
				GraphNode b = byId.get(r.toConceptId());
				if (a != null && b != null)
					edges.add(new Edge(a, b));
			}
		} catch (Exception e) {
			/* ignore */ }
	}

	private void step() {
		double k = 0.02;
		for (int i = 0; i < nodes.size(); i++) {
			GraphNode a = nodes.get(i);
			for (int j = i + 1; j < nodes.size(); j++) {
				GraphNode b = nodes.get(j);
				double dx = b.x - a.x, dy = b.y - a.y;
				double d2 = Math.max(100, dx * dx + dy * dy);
				double f = 20000.0 / d2;
				double d = Math.sqrt(d2);
				double fx = dx / d * f, fy = dy / d * f;
				a.vx -= fx;
				a.vy -= fy;
				b.vx += fx;
				b.vy += fy;
			}
		}
		for (Edge e : edges) {
			double dx = e.b.x - e.a.x, dy = e.b.y - e.a.y;
			double d = Math.max(1, Math.sqrt(dx * dx + dy * dy));
			double target = 120;
			double f = (d - target) * 0.02;
			double fx = dx / d * f, fy = dy / d * f;
			e.a.vx += fx;
			e.a.vy += fy;
			e.b.vx -= fx;
			e.b.vy -= fy;
		}
		double cx = 500, cy = 350;
		for (GraphNode n : nodes) {
			n.vx += (cx - n.x) * 0.002;
			n.vy += (cy - n.y) * 0.002;
			n.vx *= 0.85;
			n.vy *= 0.85;
			n.x += n.vx * k;
			n.y += n.vy * k;
		}
	}

	private void draw() {
		GraphicsContext g = canvas.getGraphicsContext2D();
		g.setFill(Color.web("#0b0f1a"));
		g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
		g.save();
		g.translate(offsetX, offsetY);
		g.scale(scale, scale);

		g.setStroke(Color.web("#4c5b7a"));
		g.setLineWidth(1);
		for (Edge e : edges)
			g.strokeLine(e.a.x, e.a.y, e.b.x, e.b.y);

		for (GraphNode n : nodes) {
			double r = 5 + Math.min(20, n.mentions);
			g.setFill(Color.web("#5b8cff"));
			g.fillOval(n.x - r, n.y - r, r * 2, r * 2);
			g.setFill(Color.web("#c7d3ee"));
			g.setFont(Font.font(11));
			g.setTextAlign(TextAlignment.CENTER);
			g.fillText(n.label, n.x, n.y - r - 4);
		}
		g.restore();

		if (nodes.isEmpty()) {
			g.setFill(Color.web("#6f7d99"));
			g.setFont(Font.font(16));
			g.fillText(Localization.t("empty.no_graph"), 40, 60);
		}
	}

	/** Returns the JavaFX Node root of this page. */
	public Node node() {
		return root;
	}
}