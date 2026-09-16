package learning;

import ai.AIProvider;
import database.Database;
import models.Models.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/** Asks the AI to identify what the system does NOT yet know about the topic. */
public final class KnowledgeGapDetector {
    private final AIProvider ai;
    private final Database db;
    public KnowledgeGapDetector(AIProvider ai, Database db) { this.ai = ai; this.db = db; }

    private static final String SYSTEM = """
        You analyze a research knowledge state and identify KNOWLEDGE GAPS.
        Respond ONLY as JSON:
        { "gaps": [ {"description":"...","priority":"HIGH|MEDIUM|LOW","reason":"..."} ] }
        Only list genuine gaps — subtopics, unanswered mechanisms, edge cases, unresolved conflicts.
        Return at most 5. No prose.
        """;

    public CompletableFuture<List<KnowledgeGap>> detect(Topic topic, int maxGaps) {
        try {
            List<Concept> concepts = db.listConcepts(topic.id());
            List<KnowledgeItem> knowledge = db.listKnowledge(topic.id(), null);
            List<KnowledgeGap> existing = db.listGaps(topic.id());

            StringBuilder sb = new StringBuilder();
            sb.append("TOPIC: ").append(topic.name()).append("\n\nCONCEPTS KNOWN:\n");
            sb.append(concepts.stream().limit(40).map(Concept::name).collect(Collectors.joining(", "))).append("\n\n");
            sb.append("KNOWLEDGE TITLES:\n");
            sb.append(knowledge.stream().limit(30).map(KnowledgeItem::title).collect(Collectors.joining("\n"))).append("\n\n");
            sb.append("EXISTING GAPS (do not repeat):\n");
            sb.append(existing.stream().limit(20).map(KnowledgeGap::description).collect(Collectors.joining("\n")));
            sb.append("\n\nReturn at most ").append(maxGaps).append(" NEW gaps.");

            return ai.complete(SYSTEM, sb.toString(), 0.3, 900).thenApply(raw -> {
                String cleaned = stripFences(raw);
                List<KnowledgeGap> out = new ArrayList<>();
                try {
                    Map<String,Object> m = utils.Json.asMap(utils.Json.parse(cleaned));
                    for (Object o : utils.Json.asList(m.get("gaps"))) {
                        Map<String,Object> gm = utils.Json.asMap(o);
                        String desc = utils.Json.str(gm, "description");
                        String prio = utils.Json.str(gm, "priority");
                        String reason = utils.Json.str(gm, "reason");
                        if (desc == null || desc.isBlank()) continue;
                        if (db.insertGap(topic.id(), desc, prio == null ? "MEDIUM" : prio, reason)) {
                            out.add(new KnowledgeGap(0, topic.id(), desc, prio, reason, "OPEN", java.time.Instant.now()));
                        }
                    }
                } catch (Exception e) { /* ignore parse failures */ }
                return out;
            });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private static String stripFences(String s) {
        s = s.trim();
        if (s.startsWith("```")) { int nl = s.indexOf('\n'); if (nl > 0) s = s.substring(nl + 1); if (s.endsWith("```")) s = s.substring(0, s.length()-3); }
        return s.trim();
    }
}