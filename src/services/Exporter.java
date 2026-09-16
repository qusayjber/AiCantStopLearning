package services;

import database.Database;
import models.Models.*;
import utils.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Exports a topic's research state. JSON and Markdown produce one file;
 * CSV produces a directory containing one file per entity type.
 */
public final class Exporter {
    private Exporter() {}

    public enum Format { JSON, MARKDOWN, CSV }

    // ---------- JSON ----------

    public static void exportJson(Database db, Topic t, Path out) throws Exception {
        Map<String,Object> root = new LinkedHashMap<>();
        root.put("topic", topicMap(t));
        root.put("exportedAt", java.time.Instant.now().toString());
        root.put("knowledge",     items(db.listKnowledge(t.id(), null)));
        root.put("sources",       sources(db.listSources(t.id())));
        root.put("concepts",      concepts(db.listConcepts(t.id())));
        root.put("relationships", rels(db.listRelationships(t.id())));
        root.put("questions",     questions(db.listQuestions(t.id(), null)));
        root.put("gaps",          gaps(db.listGaps(t.id())));
        root.put("contradictions", contradictions(db.listContradictions(t.id())));
        root.put("events",        events(db.listEvents(t.id(), 1000)));
        Files.writeString(out, Json.write(root), StandardCharsets.UTF_8);
    }

    // ---------- Markdown ----------

    public static void exportMarkdown(Database db, Topic t, Path out) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(t.name()).append("\n\n");
        sb.append("_Exported ").append(java.time.Instant.now()).append("_\n\n");

        sb.append("## Summary\n\n");
        Map<String,Integer> stats = db.stats(t.id());
        for (var e : stats.entrySet()) sb.append("- **").append(e.getKey()).append(":** ").append(e.getValue()).append("\n");

        sb.append("\n## Knowledge\n\n");
        for (KnowledgeItem k : db.listKnowledge(t.id(), null)) {
            sb.append("### ").append(k.title()).append("\n\n");
            sb.append("_confidence ").append(String.format("%.2f", k.confidence()))
              .append(" · ").append(k.status()).append("_\n\n");
            if (k.summary() != null && !k.summary().isBlank())
                sb.append(k.summary()).append("\n\n");
            if (k.body() != null && !k.body().isBlank())
                sb.append(k.body()).append("\n\n");
        }

        sb.append("\n## Open Questions\n\n");
        for (ResearchQuestion q : db.listQuestions(t.id(), null)) {
            sb.append("- **[").append(q.priority()).append("]** ").append(q.text());
            if (q.answer() != null && !q.answer().isBlank())
                sb.append("\n  - _Answer:_ ").append(q.answer().replace("\n", " "));
            sb.append("\n");
        }

        sb.append("\n## Knowledge Gaps\n\n");
        for (KnowledgeGap g : db.listGaps(t.id()))
            sb.append("- **[").append(g.priority()).append("]** ").append(g.description())
              .append(g.reason() == null ? "" : " — " + g.reason()).append("\n");

        sb.append("\n## Contradictions\n\n");
        var contras = db.listContradictions(t.id());
        if (contras.isEmpty()) sb.append("_None detected._\n");
        for (Contradiction c : contras)
            sb.append("- ").append(c.description())
              .append(" (items #").append(c.itemA()).append(" vs #").append(c.itemB()).append(")\n");

        sb.append("\n## Sources\n\n");
        for (Source s : db.listSources(t.id()))
            sb.append("- [").append(s.title() == null ? s.url() : s.title()).append("](")
              .append(s.url()).append(") — ").append(s.status()).append("\n");

        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
    }

    // ---------- CSV (directory of files) ----------

    public static void exportCsv(Database db, Topic t, Path dir) throws Exception {
        Files.createDirectories(dir);
        writeCsv(dir.resolve("knowledge.csv"),
            List.of("id","title","summary","confidence","status","created_at"),
            db.listKnowledge(t.id(), null).stream().map(k -> List.of(
                String.valueOf(k.id()), nz(k.title()), nz(k.summary()),
                String.valueOf(k.confidence()), nz(k.status()), k.createdAt().toString()))
            .toList());

        writeCsv(dir.resolve("sources.csv"),
            List.of("id","title","url","kind","status","credibility","discovered_at"),
            db.listSources(t.id()).stream().map(s -> List.of(
                String.valueOf(s.id()), nz(s.title()), nz(s.url()), nz(s.kind()),
                nz(s.status()), String.valueOf(s.credibility()), s.discoveredAt().toString()))
            .toList());

        writeCsv(dir.resolve("questions.csv"),
            List.of("id","text","priority","status","answer"),
            db.listQuestions(t.id(), null).stream().map(q -> List.of(
                String.valueOf(q.id()), nz(q.text()), nz(q.priority()),
                nz(q.status()), nz(q.answer())))
            .toList());

        writeCsv(dir.resolve("gaps.csv"),
            List.of("id","description","priority","reason","status"),
            db.listGaps(t.id()).stream().map(g -> List.of(
                String.valueOf(g.id()), nz(g.description()), nz(g.priority()),
                nz(g.reason()), nz(g.status())))
            .toList());

        writeCsv(dir.resolve("concepts.csv"),
            List.of("id","name","mentions"),
            db.listConcepts(t.id()).stream().map(c -> List.of(
                String.valueOf(c.id()), nz(c.name()), String.valueOf(c.mentions())))
            .toList());

        writeCsv(dir.resolve("contradictions.csv"),
            List.of("id","description","item_a","item_b","status"),
            db.listContradictions(t.id()).stream().map(c -> List.of(
                String.valueOf(c.id()), nz(c.description()),
                String.valueOf(c.itemA()), String.valueOf(c.itemB()), nz(c.status())))
            .toList());
    }

    private static void writeCsv(Path out, List<String> header, List<List<String>> rows) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", header.stream().map(Exporter::csv).toList())).append("\n");
        for (List<String> row : rows)
            sb.append(String.join(",", row.stream().map(Exporter::csv).toList())).append("\n");
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
    }

    private static String csv(String s) {
        if (s == null) return "";
        boolean quote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String e = s.replace("\"", "\"\"");
        return quote ? "\"" + e + "\"" : e;
    }

    private static String nz(String s) { return s == null ? "" : s; }

    // ---------- map builders ----------

    private static Map<String,Object> topicMap(Topic t) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", t.id()); m.put("name", t.name());
        m.put("description", t.description()); m.put("status", t.status());
        m.put("createdAt", t.createdAt().toString());
        return m;
    }
    private static List<Map<String,Object>> items(List<KnowledgeItem> in) {
        List<Map<String,Object>> out = new ArrayList<>();
        for (var k : in) { Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", k.id()); m.put("title", k.title()); m.put("summary", k.summary());
            m.put("body", k.body()); m.put("confidence", k.confidence());
            m.put("status", k.status()); m.put("createdAt", k.createdAt().toString());
            out.add(m); }
        return out;
    }
    private static List<Map<String,Object>> sources(List<Source> in) {
        List<Map<String,Object>> out = new ArrayList<>();
        for (var s : in) { Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", s.id()); m.put("title", s.title()); m.put("url", s.url());
            m.put("kind", s.kind()); m.put("status", s.status());
            m.put("credibility", s.credibility()); m.put("discoveredAt", s.discoveredAt().toString());
            out.add(m); }
        return out;
    }
    private static List<Map<String,Object>> concepts(List<Concept> in) {
        List<Map<String,Object>> out = new ArrayList<>();
        for (var c : in) { Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", c.id()); m.put("name", c.name()); m.put("mentions", c.mentions());
            out.add(m); }
        return out;
    }
    private static List<Map<String,Object>> rels(List<Relationship> in) {
        List<Map<String,Object>> out = new ArrayList<>();
        for (var r : in) { Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", r.id()); m.put("from", r.fromConceptId()); m.put("to", r.toConceptId());
            m.put("kind", r.kind()); m.put("weight", r.weight());
            out.add(m); }
        return out;
    }
    private static List<Map<String,Object>> questions(List<ResearchQuestion> in) {
        List<Map<String,Object>> out = new ArrayList<>();
        for (var q : in) { Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", q.id()); m.put("text", q.text()); m.put("priority", q.priority());
            m.put("status", q.status()); m.put("answer", q.answer());
            out.add(m); }
        return out;
    }
    private static List<Map<String,Object>> gaps(List<KnowledgeGap> in) {
        List<Map<String,Object>> out = new ArrayList<>();
        for (var g : in) { Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", g.id()); m.put("description", g.description());
            m.put("priority", g.priority()); m.put("reason", g.reason());
            m.put("status", g.status());
            out.add(m); }
        return out;
    }
    private static List<Map<String,Object>> contradictions(List<Contradiction> in) {
        List<Map<String,Object>> out = new ArrayList<>();
        for (var c : in) { Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", c.id()); m.put("description", c.description());
            m.put("itemA", c.itemA()); m.put("itemB", c.itemB());
            m.put("status", c.status());
            out.add(m); }
        return out;
    }
    private static List<Map<String,Object>> events(List<LearningEvent> in) {
        List<Map<String,Object>> out = new ArrayList<>();
        for (var e : in) { Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", e.id()); m.put("kind", e.kind());
            m.put("message", e.message()); m.put("at", e.at().toString());
            out.add(m); }
        return out;
    }
}