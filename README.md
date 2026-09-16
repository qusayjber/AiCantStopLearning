# 🧠 AI Can't Stop Learning

> **An autonomous AI research system that continuously explores a subject, discovers knowledge gaps, investigates new questions, connects concepts, and evolves its knowledge base over time.**

[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-25-blue)](https://openjfx.io/)
[![SQLite](https://img.shields.io/badge/Database-SQLite-003B57?logo=sqlite)](https://www.sqlite.org/)
[![Platform](https://img.shields.io/badge/Platform-Desktop-lightgrey)](#)
[![Language](https://img.shields.io/badge/Language-English%20%7C%20العربية-purple)](#)
[![Status](https://img.shields.io/badge/Status-Active-success)](#)

---

## 🌌 What Is AI Can't Stop Learning?

**AI Can't Stop Learning** is a Java-based autonomous research and knowledge system designed around one simple idea:

> **Don't just answer questions. Keep discovering what you don't know.**

Instead of waiting for the user to ask every question, the system continuously analyzes its current knowledge, identifies gaps, generates new research questions, investigates relevant sources, extracts information, detects relationships and contradictions, and updates its persistent knowledge base.

### The learning cycle

```text
                    ┌──────────────────────┐
                    │        TOPIC         │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │      DISCOVER        │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │       SEARCH         │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │        READ          │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │      ANALYZE         │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │   EXTRACT KNOWLEDGE  │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │    UPDATE MEMORY     │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │    FIND KNOWLEDGE    │
                    │         GAPS         │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │  GENERATE QUESTIONS  │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │  SELECT NEXT TARGET │
                    └──────────┬───────────┘
                               │
                               ▼
                              ∞
```

The system is designed around a **continuous research loop**, rather than a one-shot question-and-answer workflow.

---

# ✨ Core Features

## 🤖 Autonomous Learning Engine

The central learning engine continuously:

* explores a selected topic
* searches for new information
* processes sources
* extracts concepts and facts
* identifies relationships
* detects missing knowledge
* generates research questions
* prioritizes future learning targets
* updates the knowledge base
* repeats the research cycle

The objective is not simply to accumulate text.

The objective is to build an increasingly structured understanding of a subject.

---

## 🧩 Knowledge Gap Detection

A major part of the system is determining:

> **"What don't I know yet?"**

The engine analyzes the current knowledge state and identifies areas that require further research.

Example:

```text
Topic: Distributed Systems

Known
├── Consensus
├── Replication
├── Raft
└── Paxos

Knowledge Gaps
├── Byzantine Fault Tolerance
├── Partial Synchrony
└── CRDT Limitations

Next Learning Target
└── Byzantine Fault Tolerance
```

This allows the learning process to become goal-driven instead of repeatedly searching the same information.

---

## ❓ Self-Generated Research Questions

The system can generate questions from the knowledge it already possesses.

For example:

```text
How does Raft maintain consensus?

What happens during a network partition?

What are the limitations of CRDTs?

How does Byzantine Fault Tolerance differ
from crash fault tolerance?
```

Questions can become research tasks inside the continuous learning loop.

---

## 🧠 Persistent Knowledge Base

Knowledge is stored persistently instead of disappearing when the application closes.

The system can maintain information about:

* concepts
* facts
* relationships
* research questions
* knowledge gaps
* sources
* contradictions
* learning sessions
* research events
* AI analysis

SQLite provides the local persistence layer.

---

# 🕸️ Knowledge Graph

The application can represent relationships between concepts as a graph.

Example:

```text
                 Consensus
                /         \
               /           \
            Raft           Paxos
              |               |
         Replication      Consensus
               \             /
                \           /
                Distributed
                  Systems
                      |
                     BFT
```

The goal is to transform isolated pieces of information into connected knowledge.

The graph can be explored interactively through the application.

---

# 🔎 Source Intelligence

The research system is designed to work with external sources.

Sources can be:

* discovered
* processed
* analyzed
* associated with concepts
* associated with knowledge
* compared with other sources
* tracked in the research history

Duplicate sources should be avoided where possible.

Source attribution is preserved so that discovered knowledge can be traced back to its origin.

---

# ⚔️ Contradiction Detection

Different sources may disagree.

Instead of silently choosing one source, the system can identify potential contradictions.

Conceptually:

```text
SOURCE A
     │
     ▼
  Claim X
     │
     ├──────────────┐
     │              │
     ▼              ▼
Compare          Compare
     │              │
     └──────┬───────┘
            ▼
       SOURCE B
            │
            ▼
         Claim Y
            │
            ▼
      ⚠ Contradiction
```

This allows uncertainty and disagreement to remain visible.

---

# 📊 Research Dashboard

The main dashboard provides a real-time view of the learning process.

Possible metrics include:

```text
┌─────────────────────────────────────────────┐
│              LEARNING SESSION               │
├─────────────────────────────────────────────┤
│                                             │
│ Topic              Distributed Systems      │
│ Status             ● Learning               │
│                                             │
│ Concepts           1,284                    │
│ Sources              247                    │
│ Questions             86                    │
│ Knowledge Gaps        31                    │
│ Relationships        512                    │
│ Contradictions          8                   │
│ Research Cycles        47                   │
│                                             │
│ Current Target:                             │
│ Byzantine Fault Tolerance                   │
│                                             │
└─────────────────────────────────────────────┘
```

All statistics should be generated from actual application data rather than hardcoded demo values.

---

# 🌍 English + العربية

The application is designed with bilingual support.

### English

```text
Dashboard
Knowledge
Questions
Knowledge Gaps
Sources
Learning History
Settings
```

### العربية

```text
لوحة التحكم
المعرفة
الأسئلة
فجوات المعرفة
المصادر
سجل التعلم
الإعدادات
```

Arabic mode uses a proper **Right-to-Left (RTL)** interface.

English uses **Left-to-Right (LTR)**.

The selected language is persisted between sessions.

---

# 🎨 Light + Dark Mode

The interface supports both:

### ☀️ Light Mode

Designed as a dedicated visual theme rather than simply removing the dark background.

### 🌙 Dark Mode

A futuristic research environment optimized for long sessions.

The theme can be changed from the application settings and persists between launches.

---

# 🖥️ User Interface

The UI is built with JavaFX and focuses on:

* modern desktop UX
* responsive layouts
* smooth transitions
* animated status indicators
* knowledge cards
* interactive graph visualization
* research timelines
* live learning activity
* clean typography
* bilingual layouts
* light/dark themes

The visual concept is inspired by:

> **AI Research Laboratory + Knowledge Operating System**

---

# ⚡ Asynchronous Learning

Continuous research must never freeze the JavaFX interface.

The application architecture is designed around background execution using Java concurrency mechanisms such as:

* `ExecutorService`
* `CompletableFuture`
* Virtual Threads where appropriate

The UI remains responsive while the learning engine performs network, AI, and database operations.

---

# 🏗️ Architecture

The project follows a modular architecture.

```text
AI Can't Stop Learning
│
├── app
│   └── Application Entry Point
│
├── ai
│   ├── AIProvider
│   ├── AIClient
│   ├── KnowledgeExtractor
│   ├── QuestionGenerator
│   └── GapDetector
│
├── learning
│   ├── LearningEngine
│   ├── LearningLoop
│   ├── LearningPlanner
│   └── ResearchTask
│
├── search
│   ├── SearchProvider
│   ├── SearchEngine
│   └── WebReader
│
├── memory
│   ├── KnowledgeBase
│   ├── KnowledgeGraph
│   └── MemoryManager
│
├── database
│   └── SQLiteManager
│
├── graph
│   └── Graph Visualization
│
├── models
│   ├── Topic
│   ├── KnowledgeItem
│   ├── Concept
│   ├── Relationship
│   ├── Source
│   ├── ResearchQuestion
│   └── KnowledgeGap
│
├── ui
│   ├── Dashboard
│   ├── LearningView
│   ├── KnowledgeView
│   ├── SourcesView
│   ├── QuestionsView
│   ├── GapsView
│   ├── GraphView
│   └── SettingsView
│
├── settings
│   └── ApplicationSettings
│
├── localization
│   ├── English
│   └── Arabic
│
└── utils
```

---

# 🔄 Continuous Learning Architecture

The core concept can be represented as:

```text
┌─────────────────────┐
│   Current Knowledge │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Knowledge Gap       │
│ Detection           │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Research Questions  │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Research Planning   │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Search & Discovery  │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Source Processing   │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ AI Analysis         │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Knowledge Update    │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Knowledge Graph     │
└──────────┬──────────┘
           │
           └───────────────► Repeat
```

---

# 🛠️ Technology Stack

| Technology           | Purpose                      |
| -------------------- | ---------------------------- |
| **Java 25 LTS**      | Core application             |
| **JavaFX**           | Desktop UI                   |
| **SQLite**           | Local knowledge persistence  |
| **Java HttpClient**  | HTTP communication           |
| **JSON**             | API communication            |
| **Java Concurrency** | Background learning          |
| **Virtual Threads**  | Lightweight concurrent tasks |
| **CSS**              | JavaFX visual styling        |

---

# 🚀 Getting Started

## Requirements

Before running the project, install:

* Java 25 LTS
* JavaFX SDK
* Git

The project is designed as a pure Java project without Maven or Gradle.

---

## Clone

```bash
git clone https://github.com/qusayjber/AiCantStopLearning.git
```

```bash
cd AiCantStopLearning
```

---

## Configure JavaFX

Configure your IDE or Java runtime with the JavaFX SDK.

Example:

```text
--module-path /path/to/javafx-sdk/lib
--add-modules javafx.controls,javafx.graphics
```

Adjust the path according to your local JavaFX installation.

---

# 🔐 AI Configuration

The application is designed around an AI provider abstraction.

Conceptually:

```text
AIProvider
│
├── OpenAIProvider
├── CustomProvider
└── LocalProvider
```

This makes it possible to add different AI backends without rewriting the learning engine.

API keys should be provided through configuration.

**Never commit API keys or secrets to GitHub.**

---

# 🔎 Search Configuration

The research engine uses a search-provider abstraction.

```text
SearchProvider
│
├── WebSearchProvider
└── CustomSearchProvider
```

Search credentials should be configured locally.

Never hardcode credentials into source code.

---

# ▶️ Running the Application

After configuring Java and JavaFX, run the main application class from your IDE.

The first-run workflow should allow the user to:

```text
1. Start Application
        ↓
2. Choose Language
        ↓
3. Choose Theme
        ↓
4. Configure AI
        ↓
5. Configure Search
        ↓
6. Enter a Topic
        ↓
7. Start Learning
```

Example topic:

```text
Distributed Systems
```

---

# 🧪 Example Learning Session

Start with:

```text
Topic:
Artificial Intelligence
```

The system may discover:

```text
Machine Learning
Deep Learning
Neural Networks
Transformers
Reinforcement Learning
Generative AI
AI Agents
```

Then it may identify a gap:

```text
Knowledge Gap:
Mixture-of-Experts architectures
```

It generates research questions:

```text
How do Mixture-of-Experts models work?

Why are sparse models useful?

How does routing work?

What are the limitations?
```

The engine researches those questions and integrates the resulting knowledge.

Then it searches for the next gap.

And continues.

---

# 📚 Knowledge Model

A simplified conceptual model:

```text
Topic
 │
 ├── Knowledge
 │     ├── Concepts
 │     ├── Facts
 │     └── Relationships
 │
 ├── Sources
 │
 ├── Questions
 │
 ├── Knowledge Gaps
 │
 └── Contradictions
```

This allows the system to maintain a structured research state instead of storing only raw text.

---

# 🧭 Research History

Every research session can maintain a chronological activity timeline.

Example:

```text
16:42  Started learning session
16:43  Searching for sources
16:44  Discovered 12 sources
16:46  Processing sources
16:48  Extracted 37 concepts
16:49  Added 24 relationships
16:51  Detected knowledge gap
16:52  Generated research questions
16:54  Started next research cycle
```

This makes the autonomous process observable.

---

# ⏯️ Learning Controls

The user can control the learning engine:

```text
▶ START
⏸ PAUSE
▶ RESUME
■ STOP
```

Additional controls can include:

```text
Learning Speed
├── Low
├── Normal
├── High
└── Maximum

Research Depth
├── Quick
├── Normal
├── Deep
└── Extreme
```

---

# 📦 Export

Research data can be exported for further analysis.

Supported formats may include:

```text
JSON
Markdown
TXT
CSV
```

Possible exports:

* knowledge
* sources
* research questions
* knowledge gaps
* relationships
* research history

---

# ⌨️ Command Palette

Use:

```text
Ctrl + K
```

to open the command palette.

Possible commands:

```text
Start Learning
Pause Learning
Resume Learning
Stop Learning

New Topic

Search Knowledge

Open Knowledge Graph
Open Questions
Open Knowledge Gaps

Toggle Theme
Toggle Language

Settings
```

---

# 🔒 Security Principles

The project follows basic security principles:

* never expose API keys
* never log secrets
* use prepared SQL statements
* validate external URLs
* prefer HTTPS
* isolate external content from application logic
* handle untrusted source content safely
* gracefully handle external API failures

---

# ⚠️ Important Note

**AI Can't Stop Learning is not a foundation model that trains itself from scratch.**

The project is an autonomous research and knowledge-management system that combines:

```text
AI Models
+
Web Research
+
Persistent Memory
+
Knowledge Graphs
+
Question Generation
+
Knowledge Gap Detection
+
Continuous Research
```

The intelligence of the system therefore depends partly on the configured AI and search providers.

The goal is to create an architecture where the AI can continuously **research, organize, question, and expand its knowledge** rather than simply answer isolated prompts.

---

# 🗺️ Roadmap

## Phase 1 — Foundation

* [x] Java desktop application
* [x] JavaFX interface
* [x] SQLite persistence
* [x] Bilingual architecture
* [x] Light/Dark themes
* [x] Modular architecture

## Phase 2 — Intelligence

* [x] AI provider architecture
* [x] Search provider architecture
* [x] Knowledge extraction
* [x] Research questions
* [x] Knowledge gaps
* [x] Continuous learning loop

## Phase 3 — Knowledge

* [x] Persistent knowledge
* [x] Source tracking
* [x] Knowledge relationships
* [x] Contradiction detection
* [x] Knowledge graph

## Phase 4 — Advanced Research

* [ ] Multi-source evidence scoring
* [ ] Advanced semantic search
* [ ] Local embeddings
* [ ] Vector database support
* [ ] Improved knowledge graph reasoning
* [ ] Automatic research planning
* [ ] Long-term topic evolution
* [ ] Local AI model support

## Phase 5 — Future

* [ ] Plugin architecture
* [ ] Multiple simultaneous learning agents
* [ ] Agent collaboration
* [ ] Research reports
* [ ] Automatic citations
* [ ] Knowledge versioning
* [ ] Distributed learning
* [ ] Optional cloud synchronization

---

# 🧠 Future Vision

The long-term vision is to evolve the project from:

```text
AI Assistant
```

into:

```text
AI Research System
```

and eventually:

```text
             ┌─────────────────────┐
             │   RESEARCH AGENT    │
             └──────────┬──────────┘
                        │
            ┌───────────┼───────────┐
            ▼           ▼           ▼
        Search       Reason       Memory
            │           │           │
            └───────────┼───────────┘
                        ▼
                Knowledge Graph
                        │
                        ▼
                 Knowledge Gaps
                        │
                        ▼
                Research Planner
                        │
                        ▼
                   New Research
                        │
                        └───────► ∞
```

The ultimate goal is to create a system that continuously improves its **research coverage and organization of knowledge**, while keeping sources, uncertainty, and reasoning traceable.

---

# 🎯 Why This Project?

Most AI applications follow:

```text
User
 ↓
Question
 ↓
AI
 ↓
Answer
```

AI Can't Stop Learning explores a different model:

```text
User
 ↓
Topic
 ↓
AI
 ↓
Research
 ↓
Knowledge
 ↓
Unknowns
 ↓
Questions
 ↓
Research
 ↓
More Knowledge
 ↓
More Unknowns
 ↓
∞
```

The project is an exploration of:

* autonomous agents
* AI research systems
* knowledge representation
* knowledge graphs
* continuous learning
* information retrieval
* AI memory
* human-AI interaction
* intelligent research workflows

---

# 📸 Screenshots

Add screenshots of the application here.

Recommended screenshots:

```text
1. Dashboard
2. Active Learning Session
3. Knowledge Graph
4. Knowledge Explorer
5. Knowledge Gaps
6. Research Questions
7. Source Explorer
8. Arabic RTL Interface
9. Light Mode
10. Dark Mode
```

---

# 🤝 Contributing

Contributions, ideas, bug reports, and architectural discussions are welcome.

Possible contribution areas:

* AI providers
* Search providers
* Knowledge graph algorithms
* UI/UX
* Localization
* Performance
* Database architecture
* Research planning
* Knowledge extraction
* Testing

---

# 📄 License

Add your preferred open-source license here.

For example:

```text
MIT License
```

---

# 👨‍💻 Author

**Qusai Jaber**

Computer Science Developer

GitHub:

**https://github.com/qusayjber**

---

# ⭐ Support

If you find the project interesting:

⭐ Star the repository

🐛 Report bugs

💡 Suggest ideas

🔧 Contribute improvements

📖 Improve the documentation

---

# 🚀 AI Can't Stop Learning

> **Don't just teach the AI what to learn.**
>
> **Teach it how to discover what it doesn't know.**

```text
             LEARN
               ↓
            UNDERSTAND
               ↓
             QUESTION
               ↓
             SEARCH
               ↓
             DISCOVER
               ↓
             CONNECT
               ↓
             REMEMBER
               ↓
          FIND THE UNKNOWN
               ↓
               ∞
```

**AI Can't Stop Learning — because every answer can reveal a new question.**
