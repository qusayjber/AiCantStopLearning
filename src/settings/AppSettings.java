package settings;

import utils.Json;
import utils.Log;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Persisted user settings. Stored under ~/.aicantsl/settings.json. */
public final class AppSettings {
    private static final Path DIR = Paths.get(System.getProperty("user.home"), ".aicantsl");
    private static final Path FILE = DIR.resolve("settings.json");

    private final Map<String,Object> data = new LinkedHashMap<>();

    public AppSettings() {
        try {
            Files.createDirectories(DIR);
            if (Files.exists(FILE)) {
                Object parsed = Json.parse(Files.readString(FILE));
                data.putAll(Json.asMap(parsed));
            }
        } catch (Exception e) {
            Log.warn("Settings load failed: " + Log.redact(e.getMessage()));
        }
    }

    public String get(String key, String def) {
        Object v = data.get(key);
        return v == null ? def : v.toString();
    }
    public double getDouble(String key, double def) {
        Object v = data.get(key);
        return v instanceof Number n ? n.doubleValue() : def;
    }
    public int getInt(String key, int def) {
        Object v = data.get(key);
        return v instanceof Number n ? n.intValue() : def;
    }
    public boolean getBool(String key, boolean def) {
        Object v = data.get(key);
        return v instanceof Boolean b ? b : def;
    }
    public void put(String key, Object v) { data.put(key, v); }

    public void save() {
        try {
            Files.createDirectories(DIR);
            Files.writeString(FILE, Json.write(data));
        } catch (IOException e) {
            Log.warn("Settings save failed: " + Log.redact(e.getMessage()));
        }
    }

    // convenience
    public String language()  { return get("ui.language", "en"); }
    public String theme()     { return get("ui.theme", "dark"); }
    public String aiEndpoint(){ return get("ai.endpoint", "https://api.openai.com/v1"); }
    public String aiKey()     { return get("ai.key", ""); }
    public String aiModel()   { return get("ai.model", "gpt-4o-mini"); }
    public double aiTemp()    { return getDouble("ai.temperature", 0.2); }
    public int aiMaxTokens()  { return getInt("ai.maxTokens", 1500); }
    public String searchKind(){ return get("search.kind", "duckduckgo"); }
    public String searchEndpoint(){ return get("search.endpoint", ""); }
    public String searchKey() { return get("search.key", ""); }
    public String learningSpeed() { return get("learning.speed", "NORMAL"); }
    public String researchDepth() { return get("learning.depth", "NORMAL"); }
    public long topicId()     { return getInt("ui.topicId", -1); }
}