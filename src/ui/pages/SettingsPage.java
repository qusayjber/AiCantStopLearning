package ui.pages;

import app.AppContext;
import localization.Localization;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class SettingsPage {
    private final AppContext ctx;
    private final Runnable onSave;
    private final ScrollPane root = new ScrollPane();
    private final VBox form = new VBox(14);

    public SettingsPage(AppContext ctx, Runnable onSave) {
        this.ctx = ctx;
        this.onSave = onSave;
        form.setPadding(new Insets(24));
        form.getStyleClass().add("page");
        root.setContent(form);
        root.setFitToWidth(true);
        buildForm();
    }

    private void buildForm() {
        form.getChildren().clear();
        var s = ctx.settings;

        // ---- General
        form.getChildren().add(section(Localization.t("settings.general")));
        TextField language = new TextField(s.language());
        language.setPromptText("en / ar");
        form.getChildren().add(field(Localization.t("settings.language_code"), language));

        ChoiceBox<String> theme = new ChoiceBox<>();
        theme.getItems().addAll("dark", "light");
        theme.setValue(s.theme());
        form.getChildren().add(field(Localization.t("settings.theme"), theme));

        // ---- AI provider
        form.getChildren().add(section(Localization.t("settings.ai")));
        TextField endpoint = new TextField(s.aiEndpoint());
        TextField key = new PasswordField();
        key.setText(s.aiKey());
        TextField model = new TextField(s.aiModel());
        Slider temp = new Slider(0, 1.5, s.aiTemp());
        temp.setShowTickLabels(true); temp.setShowTickMarks(true);
        Spinner<Integer> tokens = new Spinner<>(128, 32000, s.aiMaxTokens(), 128);

        form.getChildren().addAll(
                field(Localization.t("settings.ai_endpoint"), endpoint),
                field(Localization.t("settings.ai_key"), key),
                field(Localization.t("settings.ai_model"), model),
                field(Localization.t("settings.ai_temperature"), temp),
                field(Localization.t("settings.ai_max_tokens"), tokens));

        // ---- Search provider
        form.getChildren().add(section(Localization.t("settings.search")));
        ChoiceBox<String> kind = new ChoiceBox<>();
        kind.getItems().addAll("duckduckgo", "custom");
        kind.setValue(s.searchKind());
        TextField searchEndpoint = new TextField(s.searchEndpoint());
        TextField searchKey = new PasswordField();
        searchKey.setText(s.searchKey());
        form.getChildren().addAll(
                field(Localization.t("settings.search_kind"), kind),
                field(Localization.t("settings.search_endpoint"), searchEndpoint),
                field(Localization.t("settings.search_key"), searchKey));

        // ---- Learning
        form.getChildren().add(section(Localization.t("settings.learning")));
        ChoiceBox<String> speed = new ChoiceBox<>();
        speed.getItems().addAll("LOW", "NORMAL", "HIGH", "MAXIMUM");
        speed.setValue(s.learningSpeed());
        ChoiceBox<String> depth = new ChoiceBox<>();
        depth.getItems().addAll("QUICK", "NORMAL", "DEEP", "EXTREME");
        depth.setValue(s.researchDepth());
        form.getChildren().addAll(
                field(Localization.t("settings.speed"), speed),
                field(Localization.t("settings.depth"), depth));

        // ---- Save
        Button save = new Button(Localization.t("action.save"));
        save.getStyleClass().add("btn-primary");
        save.setOnAction(e -> {
            s.put("ui.language", language.getText().trim());
            s.put("ui.theme", theme.getValue());
            s.put("ai.endpoint", endpoint.getText().trim());
            s.put("ai.key", key.getText());
            s.put("ai.model", model.getText().trim());
            s.put("ai.temperature", temp.getValue());
            s.put("ai.maxTokens", tokens.getValue());
            s.put("search.kind", kind.getValue());
            s.put("search.endpoint", searchEndpoint.getText().trim());
            s.put("search.key", searchKey.getText());
            s.put("learning.speed", speed.getValue());
            s.put("learning.depth", depth.getValue());
            s.save();
            Localization.setLocale(java.util.Locale.forLanguageTag(language.getText().trim()));
            if (onSave != null) onSave.run();
        });

        HBox actions = new HBox(8, save);
        actions.setAlignment(Pos.CENTER_RIGHT);
        form.getChildren().add(actions);
    }

    private Label section(String title) {
        Label l = new Label(title);
        l.getStyleClass().add("settings-section");
        VBox.setMargin(l, new Insets(12, 0, 4, 0));
        return l;
    }
    private VBox field(String label, Node input) {
        Label l = new Label(label);
        l.getStyleClass().add("settings-label");
        if (input instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
        return new VBox(4, l, input);
    }

    public Node node() { return root; }
}