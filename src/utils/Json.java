package utils;

import java.util.*;

/** Minimal dependency-free JSON parser/writer. */
public final class Json {
    private Json() {}

    public static Object parse(String s) { return new P(s).value(); }

    public static String write(Object o) {
        StringBuilder sb = new StringBuilder();
        w(o, sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void w(Object o, StringBuilder sb) {
        if (o == null) { sb.append("null"); return; }
        if (o instanceof String s) { esc(s, sb); return; }
        if (o instanceof Number || o instanceof Boolean) { sb.append(o); return; }
        if (o instanceof Map<?,?> m) {
            sb.append('{'); boolean f = true;
            for (var e : m.entrySet()) {
                if (!f) sb.append(','); f = false;
                esc(String.valueOf(e.getKey()), sb); sb.append(':'); w(e.getValue(), sb);
            }
            sb.append('}'); return;
        }
        if (o instanceof Iterable<?> it) {
            sb.append('['); boolean f = true;
            for (Object v : it) { if (!f) sb.append(','); f = false; w(v, sb); }
            sb.append(']'); return;
        }
        esc(o.toString(), sb);
    }

    private static void esc(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int)c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    private static final class P {
        private final String s; private int i;
        P(String s) { this.s = s; }
        Object value() {
            ws();
            if (i >= s.length()) return null;
            char c = s.charAt(i);
            return switch (c) {
                case '{' -> obj();
                case '[' -> arr();
                case '"' -> str();
                case 't','f' -> bool();
                case 'n' -> { i += 4; yield null; }
                default -> num();
            };
        }
        void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        Map<String,Object> obj() {
            Map<String,Object> m = new LinkedHashMap<>(); i++; ws();
            if (peek() == '}') { i++; return m; }
            while (true) {
                ws(); String k = str(); ws(); i++; // :
                m.put(k, value()); ws();
                char c = s.charAt(i++);
                if (c == '}') return m;
            }
        }
        List<Object> arr() {
            List<Object> l = new ArrayList<>(); i++; ws();
            if (peek() == ']') { i++; return l; }
            while (true) {
                l.add(value()); ws();
                char c = s.charAt(i++);
                if (c == ']') return l;
            }
        }
        String str() {
            StringBuilder sb = new StringBuilder(); i++;
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') break;
                if (c == '\\') {
                    char n = s.charAt(i++);
                    switch (n) {
                        case 'n' -> sb.append('\n');
                        case 't' -> sb.append('\t');
                        case 'r' -> sb.append('\r');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> { sb.append((char)Integer.parseInt(s.substring(i, i+4), 16)); i += 4; }
                        default -> sb.append(n);
                    }
                } else sb.append(c);
            }
            return sb.toString();
        }
        Boolean bool() { if (s.charAt(i) == 't') { i += 4; return true; } i += 5; return false; }
        Double num() {
            int st = i;
            while (i < s.length() && "-+.eE0123456789".indexOf(s.charAt(i)) >= 0) i++;
            return Double.parseDouble(s.substring(st, i));
        }
        char peek() { return i < s.length() ? s.charAt(i) : 0; }
    }

    @SuppressWarnings("unchecked")
    public static Map<String,Object> asMap(Object o) { return o instanceof Map ? (Map<String,Object>)o : Map.of(); }
    @SuppressWarnings("unchecked")
    public static List<Object> asList(Object o) { return o instanceof List ? (List<Object>)o : List.of(); }
    public static String str(Map<String,Object> m, String k) { Object v = m.get(k); return v == null ? null : v.toString(); }
    public static double num(Map<String,Object> m, String k, double d) { Object v = m.get(k); return v instanceof Number n ? n.doubleValue() : d; }
}