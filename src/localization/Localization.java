package localization;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * EN / AR localization with instant switch and RTL handling.
 *
 * <p>All visible UI text flows through {@link #t(String, Object...)}.
 * The current locale is exposed as an observable property so pages can
 * rebuild their content when the language changes.
 *
 * <p>When the active language is Arabic, {@link #applyOrientation(Node)}
 * flips the root scene to {@code RIGHT_TO_LEFT}; English flips it back.
 *
 * <p><b>Note:</b> This class does <i>not</i> use {@code ResourceBundle.Control}.
 * That API is not supported inside named JPMS modules, so we rely on the
 * default bundle loading mechanism, which is sufficient for our needs.
 */
public final class Localization {

    private static final Map<String, ResourceBundle> BUNDLES = new HashMap<>();

    private static final ObjectProperty<Locale> locale =
            new SimpleObjectProperty<>(Locale.forLanguageTag("en"));

    public static final List<Locale> SUPPORTED = List.of(
            Locale.forLanguageTag("en"),
            Locale.forLanguageTag("ar"));

    private static final Locale FALLBACK = Locale.forLanguageTag("en");

    private Localization() {}

    private static ResourceBundle bundle(Locale l) {
        return BUNDLES.computeIfAbsent(l.toLanguageTag(), tag ->
                ResourceBundle.getBundle(
                        "localization.messages",
                        Locale.forLanguageTag(tag)));
    }

    /**
     * Switches the active locale. If the requested locale is not supported,
     * silently falls back to English.
     */
    public static void setLocale(Locale requested) {
        boolean supported = SUPPORTED.stream()
                .anyMatch(s -> s.getLanguage().equals(requested.getLanguage()));
        Locale target = supported ? requested : FALLBACK;
        locale.set(target);
    }

    public static Locale currentLocale() { return locale.get(); }

    public static ObjectProperty<Locale> localeProperty() { return locale; }

    /** Translates a key. Missing keys return {@code !key!} so they are visible. */
    public static String t(String key, Object... args) {
        try {
            String raw = bundle(locale.get()).getString(key);
            return args.length == 0 ? raw : MessageFormat.format(raw, args);
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
    }

    public static boolean isRtl() {
        return "ar".equals(locale.get().getLanguage());
    }

    /** Applies the correct node orientation for the current locale. */
    public static void applyOrientation(Node node) {
        node.setNodeOrientation(isRtl()
                ? NodeOrientation.RIGHT_TO_LEFT
                : NodeOrientation.LEFT_TO_RIGHT);
    }
}