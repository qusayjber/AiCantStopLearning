package learning;

import ai.AIProvider;
import ai.AIProviderFactory;
import database.Database;
import models.Models.*;
import search.SearchProvider;
import search.SearchProvider.SearchResult;
import search.SearchProviderFactory;
import settings.AppSettings;
import utils.Log;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Orchestrates the full learning loop. Runs on a virtual thread.
 *
 * <p>The loop is interruptible via {@link #pause()} / {@link #resume()} and
 * {@link #stop()}, and never runs unbounded work without checking the
 * {@code stopped} flag between steps.
 *
 * <p><b>Threading contract:</b> this class runs on a virtual thread, but all
 * JavaFX properties ({@code status}, {@code activity}, {@code currentTarget})
 * MUST be mutated on the JavaFX Application Thread. Every property update
 * therefore goes through {@link Platform#runLater(Runnable)} via the helper
 * methods {@link #setStatus}, {@link #setActivity} and {@link #setTarget}.
 * Direct {@code .set(...)} calls from the loop will throw
 * {@code IllegalStateException}.
 */
public final class LearningEngine {

    public enum Status { IDLE, RUNNING, PAUSED, ERROR }

    private final Database db;
    private final AppSettings settings;
    private final AIProvider ai;
    private final SearchProvider search;
    private final KnowledgeExtractor extractor;
    private final KnowledgeGapDetector gapDetector;
    private final QuestionGenerator questionGen;
    private final ContradictionDetector contradictionDetector;

    private final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.IDLE);
    private final StringProperty activity = new SimpleStringProperty("");
    private final StringProperty currentTarget = new SimpleStringProperty("");

    private final AtomicReference<Thread> worker = new AtomicReference<>();
    private final Object pauseLock = new Object();
    private volatile boolean paused = false;
    private volatile boolean stopped = false;

    private Topic activeTopic;
    private long sessionId = -1;

    public LearningEngine(Database db, AppSettings settings) {
        this.db = db;
        this.settings = settings;
        this.ai = AIProviderFactory.from(settings);
        this.search = SearchProviderFactory.from(settings);
        this.extractor = new KnowledgeExtractor(ai);
        this.gapDetector = new KnowledgeGapDetector(ai, db);
        this.questionGen = new QuestionGenerator(ai, db);
        this.contradictionDetector = new ContradictionDetector(ai, db);
    }

    public ObjectProperty<Status> statusProperty() { return status; }
    public StringProperty activityProperty() { return activity; }
    public StringProperty currentTargetProperty() { return currentTarget; }

    /** Starts the learning loop for the given topic. No-op if already running. */
    public void start(Topic topic, Consumer<String> notifier) {
        Thread existing = worker.get();
        if (existing != null && existing.isAlive()) return;
        if (topic == null) return;

        this.activeTopic = topic;
        this.stopped = false;
        this.paused = false;

        Thread t = Thread.ofVirtual().name("learning-loop").start(() -> runLoop(notifier));
        worker.set(t);
    }

    public void pause() {
        paused = true;
        setStatus(Status.PAUSED);
    }

    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
        setStatus(Status.RUNNING);
    }

    public void stop() {
        stopped = true;
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
        setStatus(Status.IDLE);
        setActivity("");
        setTarget("");
    }

    private void waitIfPaused() {
        synchronized (pauseLock) {
            while (paused && !stopped) {
                try {
                    pauseLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void runLoop(Consumer<String> notifier) {
        try {
            setStatus(Status.RUNNING);
            sessionId = db.createSession(activeTopic.id(), activeTopic.name() + " research");
            db.updateTopicStatus(activeTopic.id(), "ACTIVE");
            notify(notifier, "Research session started for " + activeTopic.name());

            // Cold-start: generate foundational questions if there are none yet.
            List<ResearchQuestion> open = db.listQuestions(activeTopic.id(), "OPEN");
            if (open.isEmpty()) {
                setActivity("Generating foundational research questions...");
                questionGen.coldStart(activeTopic).get(120, TimeUnit.SECONDS);
                open = db.listQuestions(activeTopic.id(), "OPEN");
                db.logEvent(activeTopic.id(), "questions", "Generated initial research questions");
            }

            while (!stopped) {
                waitIfPaused();
                if (stopped) break;

                ResearchQuestion question = pickNextQuestion();

                if (question == null) {
                    setActivity("Detecting knowledge gaps...");
                    List<KnowledgeGap> gaps = gapDetector.detect(activeTopic, 4).get(120, TimeUnit.SECONDS);
                    for (KnowledgeGap g : gaps) {
                        db.logEvent(activeTopic.id(), "gap", "New gap: " + g.description());
                        notify(notifier, "Knowledge gap detected: " + g.description());
                        questionGen.generate(activeTopic, g).get(120, TimeUnit.SECONDS);
                    }
                    open = db.listQuestions(activeTopic.id(), "OPEN");
                    if (open.isEmpty()) {
                        setActivity("No open questions; waiting.");
                        sleepQuiet(6000);
                        continue;
                    }
                    question = pickNextQuestion();
                    if (question == null) break;
                }

                setTarget(truncate(question.text(), 90));
                setActivity("Researching: " + truncate(question.text(), 70));
                db.setQuestionStatus(question.id(), "INVESTIGATING");

                // ---- Step 1: Search ----
                setActivity("Searching for: " + truncate(question.text(), 70));
                List<SearchResult> results = search.search(question.text(), resultsLimit())
                        .get(60, TimeUnit.SECONDS);
                db.logEvent(activeTopic.id(), "search",
                        "Found " + results.size() + " results for: " + truncate(question.text(), 80));

                if (results.isEmpty()) {
                    db.setQuestionStatus(question.id(), "OPEN");
                    db.logEvent(activeTopic.id(), "search", "No results; re-queueing question");
                    sleepQuiet(2500);
                    bumpCycle();
                    continue;
                }

                // ---- Step 2: Collect + process up to N sources ----
                int processed = 0;
                for (SearchResult r : results) {
                    if (stopped) break;
                    if (processed >= sourcesPerQuestion()) break;
                    if (db.sourceUrlExists(activeTopic.id(), r.url())) continue;

                    Source src = new Source(0, activeTopic.id(), r.title(), r.url(), "web",
                            "DISCOVERED", 0.6, credibilityFor(r.url()), null, java.time.Instant.now());
                    if (!db.upsertSource(src)) continue;

                    setActivity("Reading: " + truncate(r.title(), 70));
                    String content = fetchContent(r.url());
                    if (content == null || content.isBlank()) {
                        db.updateSourceStatus(src.id(), "FAILED", null);
                        db.logEvent(activeTopic.id(), "source", "Failed to read: " + r.url());
                        continue;
                    }
                    db.updateSourceStatus(src.id(), "PROCESSED", content);

                    setActivity("Extracting knowledge from: " + truncate(r.title(), 60));
                    try {
                        ExtractionResult ex = extractor.extract(activeTopic.name(), r.title(), content)
                                .get(180, TimeUnit.SECONDS);
                        long itemId = persistExtraction(ex);
                        processed++;
                        db.logEvent(activeTopic.id(), "extract",
                                "Extracted " + ex.concepts().size() + " concepts from "
                                        + truncate(r.title(), 60));
                        notify(notifier, "New knowledge from " + truncate(r.title(), 50));

                        try {
                            var conflicts = contradictionDetector.scan(activeTopic, itemId)
                                    .get(60, TimeUnit.SECONDS);
                            if (!conflicts.isEmpty()) {
                                notify(notifier, "Contradiction detected — "
                                        + truncate(conflicts.get(0).description(), 80));
                            }
                        } catch (Exception ce) {
                            Log.warn("Contradiction scan failed: " + Log.redact(ce.getMessage()));
                        }
                    } catch (Exception e) {
                        Log.warn("Extraction failed: " + Log.redact(e.getMessage()));
                        db.logEvent(activeTopic.id(), "error", "Extraction failed: " + e.getMessage());
                    }
                }

                // ---- Step 3: Synthesize an answer ----
                if (processed > 0) {
                    setActivity("Synthesizing answer...");
                    try {
                        String answer = synthesizeAnswer(question);
                        db.answerQuestion(question.id(), answer);
                        db.logEvent(activeTopic.id(), "answer",
                                "Answered: " + truncate(question.text(), 80));
                    } catch (Exception e) {
                        db.setQuestionStatus(question.id(), "OPEN");
                    }
                } else {
                    db.setQuestionStatus(question.id(), "BLOCKED");
                }

                bumpCycle();
                setActivity("Cycle complete. Preparing next research target...");
                sleepQuiet(1500);
            }
        } catch (Exception e) {
            Log.err("Learning loop failed: " + Log.redact(e.getMessage()));
            setStatus(Status.ERROR);
            setActivity("Error: " + Log.redact(e.getMessage()));
        } finally {
            try {
                if (sessionId > 0) db.closeSession(sessionId, stopped ? "STOPPED" : "COMPLETED");
                if (activeTopic != null) db.updateTopicStatus(activeTopic.id(), "IDLE");
            } catch (Exception ignored) { /* best effort */ }
            if (status.get() != Status.ERROR) setStatus(Status.IDLE);
        }
    }

    private long persistExtraction(ExtractionResult ex) throws Exception {
        KnowledgeItem item = new KnowledgeItem(0, activeTopic.id(),
                ex.title() == null ? "Untitled" : ex.title(),
                ex.summary() == null ? "" : ex.summary(),
                ex.body() == null ? "" : ex.body(),
                averageConfidence(ex), "UNVERIFIED", java.time.Instant.now());
        long itemId = db.insertKnowledge(item);

        Map<String, Long> conceptIds = new HashMap<>();
        for (String c : ex.concepts()) {
            if (c == null || c.isBlank()) continue;
            long cid = db.upsertConcept(activeTopic.id(), c.trim());
            conceptIds.put(c.trim(), cid);
        }

        for (Link l : ex.links()) {
            if (l.from() == null || l.to() == null) continue;
            long a = conceptIds.computeIfAbsent(l.from(), k -> safeConceptId(k));
            long b = conceptIds.computeIfAbsent(l.to(), k -> safeConceptId(k));
            if (a > 0 && b > 0) {
                db.upsertRelationship(activeTopic.id(), a, b, l.kind(), l.weight());
            }
        }
        return itemId;
    }

    private long safeConceptId(String name) {
        try {
            return db.upsertConcept(activeTopic.id(), name);
        } catch (Exception e) {
            return -1L;
        }
    }

    private double averageConfidence(ExtractionResult ex) {
        if (ex.facts().isEmpty()) return 0.5;
        double s = 0;
        for (Fact f : ex.facts()) s += f.confidence();
        return Math.max(0.1, Math.min(1.0, s / ex.facts().size()));
    }

    private String synthesizeAnswer(ResearchQuestion q) throws Exception {
        StringBuilder ctx = new StringBuilder();
        List<KnowledgeItem> items = db.listKnowledge(activeTopic.id(), null);
        for (int i = 0; i < Math.min(8, items.size()); i++) {
            KnowledgeItem k = items.get(i);
            ctx.append("- ").append(k.title()).append(": ")
                    .append(k.summary() == null ? "" : k.summary()).append("\n");
        }
        String sys = "You answer research questions concisely (2-5 sentences) using only the "
                + "supplied knowledge. If the knowledge is insufficient, say so explicitly.";
        String usr = "TOPIC: " + activeTopic.name()
                + "\nQUESTION: " + q.text()
                + "\n\nKNOWN:\n" + ctx;
        return ai.complete(sys, usr, 0.2, 500).get(90, TimeUnit.SECONDS);
    }

    private ResearchQuestion pickNextQuestion() throws Exception {
        List<ResearchQuestion> open = db.listQuestions(activeTopic.id(), "OPEN");
        return open.isEmpty() ? null : open.get(0);
    }

    private void bumpCycle() throws Exception {
        if (sessionId > 0) db.bumpSession(sessionId);
    }

    private int resultsLimit() {
        return switch (settings.learningSpeed()) {
            case "LOW"     -> 5;
            case "HIGH"    -> 15;
            case "MAXIMUM" -> 25;
            default        -> 10;
        };
    }

    private int sourcesPerQuestion() {
        return switch (settings.researchDepth()) {
            case "QUICK"   -> 1;
            case "DEEP"    -> 4;
            case "EXTREME" -> 6;
            default        -> 2;
        };
    }

    private double credibilityFor(String url) {
        if (url.contains("wikipedia.org")) return 0.8;
        if (url.contains(".edu") || url.contains(".gov")) return 0.85;
        if (url.contains("arxiv.org") || url.contains("acm.org") || url.contains("ieee.org")) return 0.9;
        if (url.contains("github.com") || url.contains("stackoverflow.com")) return 0.65;
        return 0.5;
    }

    private String fetchContent(String url) {
        try {
            var http = java.net.http.HttpClient.newBuilder()
                    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                    .connectTimeout(java.time.Duration.ofSeconds(15))
                    .build();
            var req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 AICSL/1.0")
                    .timeout(java.time.Duration.ofSeconds(30))
                    .GET()
                    .build();
            var resp = http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) return null;
            return htmlToText(resp.body());
        } catch (Exception e) {
            return null;
        }
    }

    private static String htmlToText(String html) {
        String s = html
                .replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("(?is)<noscript.*?</noscript>", " ")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'");
        s = s.replaceAll("\\s+", " ").trim();
        return s.length() > 40000 ? s.substring(0, 40000) : s;
    }

    private void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------------
    //  FX-thread dispatch helpers — never call .set() on JavaFX properties
    //  directly from the learning loop.
    // ------------------------------------------------------------------

    private void setStatus(Status s) {
        Platform.runLater(() -> status.set(s));
    }

    private void setActivity(String msg) {
        Platform.runLater(() -> activity.set(msg == null ? "" : msg));
    }

    private void setTarget(String msg) {
        Platform.runLater(() -> currentTarget.set(msg == null ? "" : msg));
    }

    private void notify(Consumer<String> notifier, String msg) {
        if (notifier != null) Platform.runLater(() -> notifier.accept(msg));
    }

    private static String truncate(String s, int n) {
        return s == null ? "" : (s.length() <= n ? s : s.substring(0, n) + "…");
    }
}