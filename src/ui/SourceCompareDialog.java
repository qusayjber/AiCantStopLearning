package ui;

import ai.AIProvider;
import app.AppContext;
import localization.Localization;
import models.Models.Source;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import utils.Log;

import java.util.concurrent.CompletableFuture;

/**
 * Shows a comparison of two sources: common facts, differences, contradictions.
 * Runs the AI call on a background thread. Preserves source attribution.
 */
public final class SourceCompareDialog {
    private SourceCompareDialog() {}

    private static final String SYSTEM = """
        Compare two documents about the same topic.
        Respond ONLY as JSON:
        {
          "common": ["fact 1","fact 2"],
          "differences": [{"source":"A|B","point":"..."}],
          "contradictions": [{"a_claim":"...","b_claim":"...","note":"..."}],
          "confidence": 0.0-1.0
        }
        Base everything strictly on the provided text. No prose.
        """;

    public static void open(Window owner, AppContext ctx, Source a, Source b) {
        if (a.content() == null || a.content().isBlank() || b.content() == null || b.content().isBlank()) {
            new Alert(Alert.AlertType.INFORMATION, Localization.t("compare.missing_content")).showAndWait();
            return;
        }

        Stage st = new Stage();
        st.initOwner(owner);
        st.initModality(Modality.WINDOW_MODAL);
        st.setTitle(Localization.t("compare.title"));
        st.setWidth(900); st.setHeight(640);

        Label headerA = new Label("A · " + nz(a.title()));
        Label headerB = new Label("B · " + nz(b.title()));
        headerA.getStyleClass().add("detail-meta");
        headerB.getStyleClass().add("detail-meta");

        TextArea result = new TextArea(Localization.t("compare.loading"));
        result.setWrapText(true);
        result.setEditable(false);
        VBox.setVgrow(result, Priority.ALWAYS);

        Button close = new Button(Localization.t("action.close"));
        close.getStyleClass().add("btn");
        close.setOnAction(e -> st.close());

        HBox actions = new HBox(8, close);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(10, headerA, headerB, new Separator(), result, actions);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("page");
        VBox.setVgrow(result, Priority.ALWAYS);

        Scene scene = new Scene(root);
        var css = SourceCompareDialog.class.getResource(
                "dark".equals(ctx.settings.theme()) ? "/styles/dark.css" : "/styles/light.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        Localization.applyOrientation(root);
        st.setScene(scene);
        st.show();

        // Fire the comparison
        AIProvider ai = ctx.aiProvider.get();
        String user = buildPrompt(a, b);
        CompletableFuture<String> fut = ai.complete(SYSTEM, user, 0.2, 1400);
        fut.whenComplete((raw, err) -> Platform.runLater(() -> {
            if (err != null) {
                Log.warn("Comparison failed: " + Log.redact(err.getMessage()));
                result.setText(Localization.t("compare.failed") + ": " + Log.redact(err.getMessage()));
            } else {
                result.setText(pretty(raw));
            }
        }));
    }

    private static String buildPrompt(Source a, Source b) {
        return "TOPIC DOCUMENTS\n\n" +
                "--- SOURCE A: " + nz(a.title()) + " (" + a.url() + ") ---\n" +
                truncate(a.content(), 6000) + "\n\n" +
                "--- SOURCE B: " + nz(b.title()) + " (" + b.url() + ") ---\n" +
                truncate(b.content(), 6000);
    }

    private static String pretty(String raw) {
        // Render the JSON as a readable multi-line summary, falling back to raw.
        try {
            var m = utils.Json.asMap(utils.Json.parse(stripFences(raw)));
            StringBuilder sb = new StringBuilder();
            sb.append("COMMON\n");
            for (Object o : utils.Json.asList(m.get("common"))) sb.append("  • ").append(o).append("\n");
            sb.append("\nDIFFERENCES\n");
            for (Object o : utils.Json.asList(m.get("differences"))) {
                var dm = utils.Json.asMap(o);
                sb.append("  • [").append(utils.Json.str(dm, "source")).append("] ")
                  .append(utils.Json.str(dm, "point")).append("\n");
            }
            sb.append("\nCONTRADICTIONS\n");
            for (Object o : utils.Json.asList(m.get("contradictions"))) {
                var cm = utils.Json.asMap(o);
                sb.append("  • A says: ").append(utils.Json.str(cm, "a_claim")).append("\n");
                sb.append("    B says: ").append(utils.Json.str(cm, "b_claim")).append("\n");
                String note = utils.Json.str(cm, "note");
                if (note != null && !note.isBlank()) sb.append("    note: ").append(note).append("\n");
            }
            sb.append("\nCONFIDENCE: ").append(String.format("%.2f",
                    utils.Json.num(m, "confidence", 0.5)));
            return sb.toString();
        } catch (Exception e) {
            return raw;
        }
    }

    private static String stripFences(String s) {
        s = s.trim();
        if (s.startsWith("```")) { int nl = s.indexOf('\n'); if (nl > 0) s = s.substring(nl + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3); }
        return s.trim();
    }
    private static String truncate(String s, int n) { return s.length() <= n ? s : s.substring(0, n); }
    private static String nz(String s) { return s == null ? "" : s; }
}