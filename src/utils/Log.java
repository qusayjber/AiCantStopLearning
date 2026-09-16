package utils;

import java.time.LocalTime;
import java.util.function.Consumer;

public final class Log {
    private static Consumer<String> sink = s -> {};
    public static void bind(Consumer<String> c) { sink = c; }
    public static void info(String m) { emit("INFO", m); }
    public static void warn(String m) { emit("WARN", m); }
    public static void err(String m) { emit("ERROR", m); }
    private static void emit(String lvl, String m) {
        String line = "[" + LocalTime.now().withNano(0) + "] [" + lvl + "] " + m;
        sink.accept(line);
        System.out.println(line);
    }
    // Redact things that look like keys
    public static String redact(String s) {
        if (s == null) return null;
        return s.replaceAll("(?i)(sk-[A-Za-z0-9]{8})[A-Za-z0-9_-]+", "$1***")
                .replaceAll("(?i)(api[_-]?key[\"'=: ]+)[^\"'\\s,]+", "$1***");
    }
}