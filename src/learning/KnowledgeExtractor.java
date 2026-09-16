package learning;

import ai.AIProvider;
import models.Models.*;
import utils.Json;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Uses AI to turn raw text into structured ExtractionResult. */
public final class KnowledgeExtractor {
    private final AIProvider ai;
    public KnowledgeExtractor(AIProvider ai) { this.ai = ai; }

    private static final String SYSTEM = """
        You are a research knowledge extractor. Given a document and a topic, respond ONLY with a JSON object:
        {
          "title": "short title",
          "summary": "1-3 sentence summary",
          "body": "the essential facts in markdown",
          "concepts": ["concept1","concept2", ...],
          "facts": [{"statement":"...","confidence":0.0-1.0}],
          "links": [{"from":"conceptA","to":"conceptB","kind":"relation label","weight":0.0-1.0}]
        }
        No prose. No markdown fences. JSON only.
        """;

    public CompletableFuture<ExtractionResult> extract(String topic, String sourceTitle, String content) {
        String user = "TOPIC: " + topic + "\nSOURCE TITLE: " + sourceTitle +
                "\n\nDOCUMENT:\n" + truncate(content, 12000);
        return ai.complete(SYSTEM, user, 0.2, 1800).thenApply(this::parse);
    }

    private ExtractionResult parse(String json) {
        String cleaned = stripFences(json);
        Map<String,Object> m = Json.asMap(Json.parse(cleaned));
        List<String> concepts = new ArrayList<>();
        for (Object o : Json.asList(m.get("concepts"))) concepts.add(String.valueOf(o));
        List<Fact> facts = new ArrayList<>();
        for (Object o : Json.asList(m.get("facts"))) {
            Map<String,Object> fm = Json.asMap(o);
            facts.add(new Fact(Json.str(fm, "statement"), Json.num(fm, "confidence", 0.5)));
        }
        List<Link> links = new ArrayList<>();
        for (Object o : Json.asList(m.get("links"))) {
            Map<String,Object> lm = Json.asMap(o);
            links.add(new Link(Json.str(lm, "from"), Json.str(lm, "to"),
                    Json.str(lm, "kind"), Json.num(lm, "weight", 1.0)));
        }
        return new ExtractionResult(
                Json.str(m, "title"),
                Json.str(m, "summary"),
                Json.str(m, "body"),
                concepts, facts, links);
    }

    private static String stripFences(String s) {
        s = s.trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl > 0) s = s.substring(nl + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }

    private static String truncate(String s, int n) { return s.length() <= n ? s : s.substring(0, n); }
}