package app;

import ai.AIProvider;
import ai.AIProviderFactory;
import database.Database;
import learning.LearningEngine;
import localization.Localization;
import search.SearchProvider;
import search.SearchProviderFactory;
import settings.AppSettings;
import utils.Log;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import models.Models.Topic;

import java.nio.file.Path;

/** Central wiring point. Owned by MainWindow. */
public final class AppContext implements AutoCloseable {
    public final AppSettings settings;
    public final Database db;
    public final ObjectProperty<Topic> activeTopic = new SimpleObjectProperty<>();
    public final ObjectProperty<AIProvider> aiProvider = new SimpleObjectProperty<>();
    public final ObjectProperty<SearchProvider> searchProvider = new SimpleObjectProperty<>();
    public final ObjectProperty<LearningEngine> engine = new SimpleObjectProperty<>();

    public AppContext() throws Exception {
        this.settings = new AppSettings();
        Path dbPath = Path.of(System.getProperty("user.home"), ".aicantsl", "knowledge.db");
        java.nio.file.Files.createDirectories(dbPath.getParent());
        this.db = new Database(dbPath.toString());
        Localization.setLocale(java.util.Locale.forLanguageTag(settings.language()));
        rebuildProviders();
    }

    public void rebuildProviders() {
        aiProvider.set(AIProviderFactory.from(settings));
        searchProvider.set(SearchProviderFactory.from(settings));
        engine.set(new LearningEngine(db, settings));
        Log.info("Providers rebuilt. AI=" + aiProvider.get().name() +
                " Search=" + searchProvider.get().name());
    }

    @Override public void close() { db.close(); }
}