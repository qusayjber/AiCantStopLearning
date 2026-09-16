package learning;

import ai.AIProvider;
import database.Database;
import models.Models.*;
import utils.Json;
import utils.Log;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Detects logical contradictions between a newly-extracted knowledge item
 * and the existing knowledge base for the topic.
 *
 * Heuristic gate: only invoke the AI when the new item overlaps lexically
 * with at least one existing item — avoids needless API calls.
 */
public final class ContradictionDetector {
    private final AIProvider ai;
    private final Database db;

    public ContradictionDetector(AIProvider ai, Database db) { this.ai = ai; this.db = db; }

    private static final String SYSTEM = """
        You are a strict logical consistency checker for a research knowledge base.
        Given a NEW knowledge item and a list of EXISTING items, identify claims that
        are logically INCOMPATIBLE with each other (not merely different, not complementary).
        Respond ONLY as JSON:
        {
          "contradictions": [
            { "description": "short description of the conflict",
              "existing_item_id": <id of the existing item in conflict>,
              "confidence": 0.0-1.0 }
          ]
        }
        If none are logically incompatible, return {"contradictions":[]}.
        No prose.
        """;

    /**
     * Scans a newly-inserted item against existing items.
     * Persists any detected contradictions and returns them.
     */
    public CompletableFuture<List<Contradiction>> scan(Topic topic, long newItemId) {
        try {
            KnowledgeItem newItem = findById(topic.id(), newItemId);
            if (newItem == null) return CompletableFuture.completedFuture(List.of());

            List<KnowledgeItem> peers = new ArrayList<>();
            for (KnowledgeItem k : db.listKnowledge(topic.id(), null)) {
                if (k.id() == newItemId) continue;
                if (lexicalOverlap(newItem, k)) peers.add(k);
            }
            if (peers.isEmpty()) return CompletableFuture.completedFuture(List.of());

            StringBuilder sb = new StringBuilder();
            sb.append("TOPIC: ").append(topic.name()).append("\n\n");
            sb.append("NEW ITEM (id=").append(newItem.id()).append("):\n")
              .append("- title: ").append(newItem.title()).append("\n")
              .append("- summary: ").append(nvl(newItem.summary())).append("\n\n");
            sb.append("EXISTING ITEMS:\n");
            for (KnowledgeItem k : peers) {
                sb.append("- id=").append(k.id())
                  .append(" | title: ").append(k.title())
                  .append(" | summary: ").append(nvl(k.summary()))
                  .append("\n");
            }

            return ai.complete(SYSTEM, sb.toString(), 0.2, 900).thenApply(raw -> {
                List<Contradiction> found = new ArrayList<>();
                try {
                    Map<String,Object> m = Json.asMap(Json.parse(stripFences(raw)));
                    for (Object o : Json.asList(m.get("contradictions"))) {
                        Map<String,Object> cm = Json.asMap(o);
                        String desc = Json.str(cm, "description");
                        double conf = Json.num(cm, "confidence", 0.5);
                        Object idRaw = cm.get("existing_item_id");
                        long otherId = idRaw instanceof Number n ? n.longValue() : -1;
                        if (desc == null || desc.isBlank() || otherId <= 0) continue;
                        if (conf < 0.4) continue;

                        db.insertContradiction(topic.id(), desc, newItemId, otherId);
                        db.logEvent(topic.id(), "contradiction",
                                "Conflict between #" + newItemId + " and #" + otherId + ": " + truncate(desc, 100));
                        found.add(new Contradiction(0, topic.id(), desc, newItemId, otherId,
                                "OPEN", java.time.Instant.now()));
                    }
                } catch (Exception e) {
                    Log.warn("Contradiction parse failed: " + Log.redact(e.getMessage()));
                }
                return found;
            });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private KnowledgeItem findById(long topicId, long id) throws Exception {
        for (KnowledgeItem k : db.listKnowledge(topicId, null)) if (k.id() == id) return k;
        return null;
    }

    /** Cheap overlap gate: shares at least one meaningful word. */
    private static boolean lexicalOverlap(KnowledgeItem a, KnowledgeItem b) {
        Set<String> wa = words(a.title() + " " + nvl(a.summary()));
        Set<String> wb = words(b.title() + " " + nvl(b.summary()));
        wa.retainAll(wb);
        return wa.size() >= 2;
    }

    private static Set<String> words(String s) {
        Set<String> out = new HashSet<>();
        for (String w : s.toLowerCase().split("[^a-z0-9]+"))
            if (w.length() >= 4) out.add(w);
        return out;
    }

    private static String nvl(String s) { return s == null ? "" : s; }
    private static String truncate(String s, int n) { return s.length() <= n ? s : s.substring(0, n) + "…"; }
    private static String stripFences(String s) {
        s = s.trim();
        if (s.startsWith("```")) { int nl = s.indexOf('\n'); if (nl > 0) s = s.substring(nl + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3); }
        return s.trim();
    }
}