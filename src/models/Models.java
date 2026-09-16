package models;

import java.time.Instant;
import java.util.List;

public final class Models {
    private Models() {}

    public record Topic(long id, String name, String description, String status, Instant createdAt) {}
    public record Source(long id, long topicId, String title, String url, String kind,
                         String status, double relevance, double credibility,
                         String content, Instant discoveredAt) {}
    public record KnowledgeItem(long id, long topicId, String title, String summary, String body,
                                double confidence, String status, Instant createdAt) {}
    public record Concept(long id, long topicId, String name, int mentions) {}
    public record Relationship(long id, long topicId, long fromConceptId, long toConceptId,
                               String kind, double weight) {}
    public record ResearchQuestion(long id, long topicId, String text, String priority,
                                   String status, String answer, Instant createdAt) {}
    public record KnowledgeGap(long id, long topicId, String description, String priority,
                               String reason, String status, Instant createdAt) {}
    public record Contradiction(long id, long topicId, String description, long itemA, long itemB,
                                String status, Instant createdAt) {}
    public record LearningEvent(long id, long topicId, String kind, String message, Instant at) {}
    public record Session(long id, long topicId, String name, String status, long cycles,
                          Instant startedAt) {}

    public record ExtractionResult(
            String title,
            String summary,
            String body,
            List<String> concepts,
            List<Fact> facts,
            List<Link> links
    ) {}

    public record Fact(String statement, double confidence) {}
    public record Link(String from, String to, String kind, double weight) {}
}