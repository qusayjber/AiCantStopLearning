package database;

import models.Models.*;
import utils.Log;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/** SQLite persistence. All writes use prepared statements. */
public final class Database implements AutoCloseable {
    private final Connection conn;

    public Database(String path) throws SQLException {
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement st = conn.createStatement()) { st.execute("PRAGMA foreign_keys=ON"); }
        migrate();
    }

    private void migrate() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS topics(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  name TEXT NOT NULL UNIQUE,
                  description TEXT,
                  status TEXT NOT NULL DEFAULT 'IDLE',
                  created_at TEXT NOT NULL
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sources(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  topic_id INTEGER NOT NULL,
                  title TEXT, url TEXT NOT NULL, kind TEXT,
                  status TEXT NOT NULL DEFAULT 'DISCOVERED',
                  relevance REAL DEFAULT 0, credibility REAL DEFAULT 0.5,
                  content TEXT,
                  discovered_at TEXT NOT NULL,
                  UNIQUE(topic_id, url),
                  FOREIGN KEY(topic_id) REFERENCES topics(id) ON DELETE CASCADE
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS knowledge(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  topic_id INTEGER NOT NULL,
                  title TEXT NOT NULL, summary TEXT, body TEXT,
                  confidence REAL DEFAULT 0.5,
                  status TEXT DEFAULT 'UNVERIFIED',
                  created_at TEXT NOT NULL,
                  FOREIGN KEY(topic_id) REFERENCES topics(id) ON DELETE CASCADE
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS concepts(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  topic_id INTEGER NOT NULL,
                  name TEXT NOT NULL, mentions INTEGER DEFAULT 1,
                  UNIQUE(topic_id, name),
                  FOREIGN KEY(topic_id) REFERENCES topics(id) ON DELETE CASCADE
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS relationships(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  topic_id INTEGER NOT NULL,
                  from_concept INTEGER NOT NULL, to_concept INTEGER NOT NULL,
                  kind TEXT, weight REAL DEFAULT 1,
                  FOREIGN KEY(topic_id) REFERENCES topics(id) ON DELETE CASCADE,
                  FOREIGN KEY(from_concept) REFERENCES concepts(id) ON DELETE CASCADE,
                  FOREIGN KEY(to_concept) REFERENCES concepts(id) ON DELETE CASCADE
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS questions(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  topic_id INTEGER NOT NULL,
                  text TEXT NOT NULL,
                  priority TEXT DEFAULT 'MEDIUM',
                  status TEXT DEFAULT 'OPEN',
                  answer TEXT,
                  created_at TEXT NOT NULL,
                  UNIQUE(topic_id, text),
                  FOREIGN KEY(topic_id) REFERENCES topics(id) ON DELETE CASCADE
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS gaps(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  topic_id INTEGER NOT NULL,
                  description TEXT NOT NULL,
                  priority TEXT DEFAULT 'MEDIUM',
                  reason TEXT, status TEXT DEFAULT 'OPEN',
                  created_at TEXT NOT NULL,
                  UNIQUE(topic_id, description),
                  FOREIGN KEY(topic_id) REFERENCES topics(id) ON DELETE CASCADE
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS contradictions(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  topic_id INTEGER NOT NULL, description TEXT,
                  item_a INTEGER, item_b INTEGER,
                  status TEXT DEFAULT 'OPEN',
                  created_at TEXT NOT NULL,
                  FOREIGN KEY(topic_id) REFERENCES topics(id) ON DELETE CASCADE
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS events(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  topic_id INTEGER, kind TEXT, message TEXT,
                  at TEXT NOT NULL
                )""");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sessions(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  topic_id INTEGER NOT NULL, name TEXT,
                  status TEXT NOT NULL, cycles INTEGER DEFAULT 0,
                  started_at TEXT NOT NULL,
                  FOREIGN KEY(topic_id) REFERENCES topics(id) ON DELETE CASCADE
                )""");
        }
    }

    // ---------- topics ----------
    public Topic createTopic(String name, String description) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO topics(name, description, status, created_at) VALUES(?,?, 'IDLE', ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description == null ? "" : description);
            ps.setString(3, Instant.now().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return getTopic(rs.getLong(1));
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM topics WHERE name=?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return getTopic(rs.getLong(1)); }
        }
        return null;
    }

    public Topic getTopic(long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM topics WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Topic(rs.getLong("id"), rs.getString("name"), rs.getString("description"),
                        rs.getString("status"), Instant.parse(rs.getString("created_at")));
            }
        }
    }

    public List<Topic> listTopics() throws SQLException {
        List<Topic> out = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM topics ORDER BY id")) {
            while (rs.next()) out.add(new Topic(rs.getLong("id"), rs.getString("name"),
                    rs.getString("description"), rs.getString("status"),
                    Instant.parse(rs.getString("created_at"))));
        }
        return out;
    }

    public void updateTopicStatus(long id, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE topics SET status=? WHERE id=?")) {
            ps.setString(1, status); ps.setLong(2, id); ps.executeUpdate();
        }
    }

    public void deleteTopic(long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM topics WHERE id=?")) {
            ps.setLong(1, id); ps.executeUpdate();
        }
    }

    // ---------- sources ----------
    public boolean upsertSource(Source s) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT OR IGNORE INTO sources(topic_id,title,url,kind,status,relevance,credibility,content,discovered_at)
            VALUES(?,?,?,?,?,?,?,?,?)""")) {
            ps.setLong(1, s.topicId());
            ps.setString(2, s.title());
            ps.setString(3, s.url());
            ps.setString(4, s.kind());
            ps.setString(5, s.status());
            ps.setDouble(6, s.relevance());
            ps.setDouble(7, s.credibility());
            ps.setString(8, s.content());
            ps.setString(9, s.discoveredAt().toString());
            return ps.executeUpdate() > 0;
        }
    }

    public void updateSourceStatus(long id, String status, String content) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE sources SET status=?, content=COALESCE(?,content) WHERE id=?")) {
            ps.setString(1, status); ps.setString(2, content); ps.setLong(3, id); ps.executeUpdate();
        }
    }

    public List<Source> listSources(long topicId) throws SQLException {
        List<Source> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM sources WHERE topic_id=? ORDER BY id DESC")) {
            ps.setLong(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Source(rs.getLong("id"), rs.getLong("topic_id"),
                        rs.getString("title"), rs.getString("url"), rs.getString("kind"),
                        rs.getString("status"), rs.getDouble("relevance"), rs.getDouble("credibility"),
                        rs.getString("content"), Instant.parse(rs.getString("discovered_at"))));
            }
        }
        return out;
    }

    public boolean sourceUrlExists(long topicId, String url) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM sources WHERE topic_id=? AND url=?")) {
            ps.setLong(1, topicId); ps.setString(2, url);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // ---------- knowledge ----------
    public long insertKnowledge(KnowledgeItem k) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT INTO knowledge(topic_id,title,summary,body,confidence,status,created_at)
            VALUES(?,?,?,?,?,?,?)""", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, k.topicId()); ps.setString(2, k.title()); ps.setString(3, k.summary());
            ps.setString(4, k.body()); ps.setDouble(5, k.confidence()); ps.setString(6, k.status());
            ps.setString(7, Instant.now().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getLong(1); }
        }
    }

    public void setKnowledgeStatus(long id, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE knowledge SET status=? WHERE id=?")) {
            ps.setString(1, status); ps.setLong(2, id); ps.executeUpdate();
        }
    }

    public void deleteKnowledge(long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM knowledge WHERE id=?")) {
            ps.setLong(1, id); ps.executeUpdate();
        }
    }

    public List<KnowledgeItem> listKnowledge(long topicId, String query) throws SQLException {
        List<KnowledgeItem> out = new ArrayList<>();
        String sql = "SELECT * FROM knowledge WHERE topic_id=?" +
                (query == null || query.isBlank() ? "" : " AND (title LIKE ? OR summary LIKE ?)") +
                " ORDER BY id DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, topicId);
            if (query != null && !query.isBlank()) {
                String q = "%" + query + "%";
                ps.setString(2, q); ps.setString(3, q);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new KnowledgeItem(rs.getLong("id"), rs.getLong("topic_id"),
                        rs.getString("title"), rs.getString("summary"), rs.getString("body"),
                        rs.getDouble("confidence"), rs.getString("status"),
                        Instant.parse(rs.getString("created_at"))));
            }
        }
        return out;
    }

    // ---------- concepts ----------
    public long upsertConcept(long topicId, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT INTO concepts(topic_id,name,mentions) VALUES(?,?,1)
            ON CONFLICT(topic_id,name) DO UPDATE SET mentions = mentions + 1""",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, topicId); ps.setString(2, name);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM concepts WHERE topic_id=? AND name=?")) {
            ps.setLong(1, topicId); ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }

    public List<Concept> listConcepts(long topicId) throws SQLException {
        List<Concept> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM concepts WHERE topic_id=? ORDER BY mentions DESC")) {
            ps.setLong(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Concept(rs.getLong("id"), rs.getLong("topic_id"),
                        rs.getString("name"), rs.getInt("mentions")));
            }
        }
        return out;
    }

    public boolean conceptExists(long topicId, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM concepts WHERE topic_id=? AND name=?")) {
            ps.setLong(1, topicId); ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // ---------- relationships ----------
    public void upsertRelationship(long topicId, long from, long to, String kind, double w) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT INTO relationships(topic_id,from_concept,to_concept,kind,weight) VALUES(?,?,?,?,?)""")) {
            ps.setLong(1, topicId); ps.setLong(2, from); ps.setLong(3, to);
            ps.setString(4, kind); ps.setDouble(5, w);
            ps.executeUpdate();
        }
    }

    public List<Relationship> listRelationships(long topicId) throws SQLException {
        List<Relationship> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM relationships WHERE topic_id=?")) {
            ps.setLong(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Relationship(rs.getLong("id"), rs.getLong("topic_id"),
                        rs.getLong("from_concept"), rs.getLong("to_concept"),
                        rs.getString("kind"), rs.getDouble("weight")));
            }
        }
        return out;
    }

    // ---------- questions ----------
    public boolean insertQuestion(long topicId, String text, String priority) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT OR IGNORE INTO questions(topic_id,text,priority,status,created_at) VALUES(?,?,?,'OPEN',?)""")) {
            ps.setLong(1, topicId); ps.setString(2, text); ps.setString(3, priority);
            ps.setString(4, Instant.now().toString());
            return ps.executeUpdate() > 0;
        }
    }

    public List<ResearchQuestion> listQuestions(long topicId, String status) throws SQLException {
        List<ResearchQuestion> out = new ArrayList<>();
        String sql = "SELECT * FROM questions WHERE topic_id=?" +
                (status == null ? "" : " AND status=?") + " ORDER BY CASE priority " +
                "WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END, id DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, topicId);
            if (status != null) ps.setString(2, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new ResearchQuestion(rs.getLong("id"), rs.getLong("topic_id"),
                        rs.getString("text"), rs.getString("priority"), rs.getString("status"),
                        rs.getString("answer"), Instant.parse(rs.getString("created_at"))));
            }
        }
        return out;
    }

    public void answerQuestion(long id, String answer) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE questions SET answer=?, status='ANSWERED' WHERE id=?")) {
            ps.setString(1, answer); ps.setLong(2, id); ps.executeUpdate();
        }
    }

    public void setQuestionStatus(long id, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE questions SET status=? WHERE id=?")) {
            ps.setString(1, status); ps.setLong(2, id); ps.executeUpdate();
        }
    }

    // ---------- gaps ----------
    public boolean insertGap(long topicId, String desc, String priority, String reason) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT OR IGNORE INTO gaps(topic_id,description,priority,reason,status,created_at)
            VALUES(?,?,?,?,'OPEN',?)""")) {
            ps.setLong(1, topicId); ps.setString(2, desc); ps.setString(3, priority);
            ps.setString(4, reason); ps.setString(5, Instant.now().toString());
            return ps.executeUpdate() > 0;
        }
    }

    public List<KnowledgeGap> listGaps(long topicId) throws SQLException {
        List<KnowledgeGap> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM gaps WHERE topic_id=? ORDER BY CASE priority " +
                        "WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END, id DESC")) {
            ps.setLong(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new KnowledgeGap(rs.getLong("id"), rs.getLong("topic_id"),
                        rs.getString("description"), rs.getString("priority"), rs.getString("reason"),
                        rs.getString("status"), Instant.parse(rs.getString("created_at"))));
            }
        }
        return out;
    }

    public void setGapStatus(long id, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE gaps SET status=? WHERE id=?")) {
            ps.setString(1, status); ps.setLong(2, id); ps.executeUpdate();
        }
    }

    // ---------- contradictions ----------
    public void insertContradiction(long topicId, String desc, long a, long b) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT INTO contradictions(topic_id,description,item_a,item_b,status,created_at)
            VALUES(?,?,?,?,'OPEN',?)""")) {
            ps.setLong(1, topicId); ps.setString(2, desc); ps.setLong(3, a); ps.setLong(4, b);
            ps.setString(5, Instant.now().toString());
            ps.executeUpdate();
        }
    }

    public List<Contradiction> listContradictions(long topicId) throws SQLException {
        List<Contradiction> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM contradictions WHERE topic_id=? ORDER BY id DESC")) {
            ps.setLong(1, topicId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Contradiction(rs.getLong("id"), rs.getLong("topic_id"),
                        rs.getString("description"), rs.getLong("item_a"), rs.getLong("item_b"),
                        rs.getString("status"), Instant.parse(rs.getString("created_at"))));
            }
        }
        return out;
    }

    // ---------- events ----------
    public void logEvent(long topicId, String kind, String message) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO events(topic_id,kind,message,at) VALUES(?,?,?,?)")) {
            ps.setLong(1, topicId); ps.setString(2, kind); ps.setString(3, message);
            ps.setString(4, Instant.now().toString());
            ps.executeUpdate();
        }
    }

    public List<LearningEvent> listEvents(long topicId, int limit) throws SQLException {
        List<LearningEvent> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM events WHERE topic_id=? ORDER BY id DESC LIMIT ?")) {
            ps.setLong(1, topicId); ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new LearningEvent(rs.getLong("id"), rs.getLong("topic_id"),
                        rs.getString("kind"), rs.getString("message"), Instant.parse(rs.getString("at"))));
            }
        }
        return out;
    }

    // ---------- stats ----------
    public Map<String,Integer> stats(long topicId) throws SQLException {
        Map<String,Integer> m = new LinkedHashMap<>();
        m.put("sources",    count("SELECT COUNT(*) FROM sources WHERE topic_id=?", topicId));
        m.put("knowledge",  count("SELECT COUNT(*) FROM knowledge WHERE topic_id=?", topicId));
        m.put("concepts",   count("SELECT COUNT(*) FROM concepts WHERE topic_id=?", topicId));
        m.put("relations",  count("SELECT COUNT(*) FROM relationships WHERE topic_id=?", topicId));
        m.put("questions",  count("SELECT COUNT(*) FROM questions WHERE topic_id=?", topicId));
        m.put("gaps",       count("SELECT COUNT(*) FROM gaps WHERE topic_id=? AND status='OPEN'", topicId));
        m.put("contradictions", count("SELECT COUNT(*) FROM contradictions WHERE topic_id=? AND status='OPEN'", topicId));
        m.put("cycles",     count("SELECT COALESCE(SUM(cycles),0) FROM sessions WHERE topic_id=?", topicId));
        return m;
    }

    private int count(String sql, long topicId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, topicId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    // ---------- sessions ----------
    public long createSession(long topicId, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sessions(topic_id,name,status,cycles,started_at) VALUES(?,?,'ACTIVE',0,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, topicId); ps.setString(2, name); ps.setString(3, Instant.now().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getLong(1); }
        }
    }

    public void bumpSession(long sessionId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sessions SET cycles = cycles + 1 WHERE id=?")) {
            ps.setLong(1, sessionId); ps.executeUpdate();
        }
    }

    public void closeSession(long sessionId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE sessions SET status=? WHERE id=?")) {
            ps.setString(1, status); ps.setLong(2, sessionId); ps.executeUpdate();
        }
    }

    @Override public void close() {
        try { conn.close(); } catch (SQLException e) { Log.warn("DB close: " + e.getMessage()); }
    }
}