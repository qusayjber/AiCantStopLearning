package learning;

import ai.AIProvider;
import database.Database;
import models.Models.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Turns gaps into concrete research questions. */
public final class QuestionGenerator {
    private final AIProvider ai;
    private final Database db;
    public QuestionGenerator(AIProvider ai, Database db) { this.ai = ai; this.db = db; }

    private static final String SYSTEM = """
        You turn research gaps into precise, answerable research questions.
        Respond ONLY as JSON: { "questions": [ {"text":"...","priority":"HIGH|MEDIUM|LOW"} ] }
        At most 6. Questions must be specific (not vague). No prose.
        """;

    public CompletableFuture<List<ResearchQuestion>> generate(Topic topic, KnowledgeGap gap) {
        String user = "TOPIC: " + topic.name() + "\nGAP: " + gap.description() + "\nREASON: " + gap.reason();
        return ai.complete(SYSTEM, user, 0.4, 700).thenApply(raw -> {
            List<ResearchQuestion> out = new ArrayList<>();
            try {
                Map<String,Object> m = utils.Json.asMap(utils.Json.parse(stripFences(raw)));
                for (Object o : utils.Json.asList(m.get("questions"))) {
                    Map<String,Object> qm = utils.Json.asMap(o);
                    String text = utils.Json.str(qm, "text");
                    String prio = utils.Json.str(qm, "priority");
                    if (text == null || text.isBlank()) continue;
                    if (db.insertQuestion(topic.id(), text, prio == null ? "MEDIUM" : prio)) {
                        out.add(new ResearchQuestion(0, topic.id(), text, prio, "OPEN", null, java.time.Instant.now()));
                    }
                }
            } catch (Exception ignored) {}
            return out;
        });
    }

    /** Free-form question generation when no gaps exist yet (cold start). */
    public CompletableFuture<List<ResearchQuestion>> coldStart(Topic topic) {
        String user = "TOPIC: " + topic.name() +
                "\nGenerate 6 fundamental research questions that should guide learning about this topic.";
        return ai.complete(SYSTEM, user, 0.5, 700).thenApply(raw -> {
            List<ResearchQuestion> out = new ArrayList<>();
            try {
                Map<String,Object> m = utils.Json.asMap(utils.Json.parse(stripFences(raw)));
                for (Object o : utils.Json.asList(m.get("questions"))) {
                    Map<String,Object> qm = utils.Json.asMap(o);
                    String text = utils.Json.str(qm, "text");
                    String prio = utils.Json.str(qm, "priority");
                    if (text == null || text.isBlank()) continue;
                    if (db.insertQuestion(topic.id(), text, prio == null ? "MEDIUM" : prio)) {
                        out.add(new ResearchQuestion(0, topic.id(), text, prio, "OPEN", null, java.time.Instant.now()));
                    }
                }
            } catch (Exception ignored) {}
            return out;
        });
    }

    private static String stripFences(String s) {
        s = s.trim();
        if (s.startsWith("```")) { int nl = s.indexOf('\n'); if (nl > 0) s = s.substring(nl + 1); if (s.endsWith("```")) s = s.substring(0, s.length()-3); }
        return s.trim();
    }
}