package ui;

import app.AppContext;
import localization.Localization;
import models.Models.Topic;
import services.Exporter;
import utils.Log;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/** Wires Exporter to a file chooser. Runs I/O on a background thread. */
public final class ExportAction {
    private ExportAction() {}

    public static void run(Window owner, AppContext ctx, java.util.function.Consumer<String> toast) {
        Topic t = ctx.activeTopic.get();
        if (t == null) { toast.accept(Localization.t("empty.no_knowledge")); return; }

        ChoiceDialog<Exporter.Format> fmt = new ChoiceDialog<>(Exporter.Format.JSON,
                List.of(Exporter.Format.JSON, Exporter.Format.MARKDOWN, Exporter.Format.CSV));
        fmt.initOwner(owner);
        fmt.setTitle(Localization.t("export.title"));
        fmt.setHeaderText(Localization.t("export.choose_format"));
        var choice = fmt.showAndWait();
        if (choice.isEmpty()) return;

        Exporter.Format format = choice.get();
        Path target = pickTarget(owner, format);
        if (target == null) return;

        Thread.ofVirtual().name("exporter").start(() -> {
            try {
                switch (format) {
                    case JSON     -> Exporter.exportJson(ctx.db, t, target);
                    case MARKDOWN -> Exporter.exportMarkdown(ctx.db, t, target);
                    case CSV      -> Exporter.exportCsv(ctx.db, t, target);
                }
                Platform.runLater(() -> toast.accept(Localization.t("export.done") + ": " + target));
            } catch (Exception e) {
                Log.err("Export failed: " + e.getMessage());
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR,
                            Localization.t("export.failed") + ": " + e.getMessage());
                    a.initOwner(owner); a.showAndWait();
                });
            }
        });
    }

    private static Path pickTarget(Window owner, Exporter.Format fmt) {
        if (fmt == Exporter.Format.CSV) {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle(Localization.t("export.choose_dir"));
            File dir = dc.showDialog(owner);
            return dir == null ? null : dir.toPath();
        }
        FileChooser fc = new FileChooser();
        fc.setTitle(Localization.t("export.choose_file"));
        String ext = fmt == Exporter.Format.JSON ? "*.json" : "*.md";
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(fmt.name(), ext));
        fc.setInitialFileName("research-export." + (fmt == Exporter.Format.JSON ? "json" : "md"));
        File f = fc.showSaveDialog(owner);
        return f == null ? null : f.toPath();
    }
}