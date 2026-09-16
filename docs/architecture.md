\# Architecture



\## High-Level Diagram



```

┌──────────────────────────────────────────────────────────────────────┐

│                        JavaFX Application Thread                     │

│                                                                      │

│   MainWindow  ──  Sidebar  ──  TopBar  ──  CommandPalette            │

│        │                                                             │

│        └──  Pages (Dashboard / Knowledge / Sources / Questions /     │

│                    Gaps / Graph / History / Settings)                │

└────────────────────────────┬─────────────────────────────────────────┘

&#x20;                            │  reads \& writes

&#x20;                            │  JavaFX properties (always on FX thread)

&#x20;                            ▼

┌──────────────────────────────────────────────────────────────────────┐

│                          AppContext (singleton)                      │

│   • holds AppSettings, Database                                     │

│   • holds ObjectProperty<Topic> activeTopic                         │

│   • holds ObjectProperty<AIProvider>                                │

│   • holds ObjectProperty<SearchProvider>                            │

│   • holds ObjectProperty<LearningEngine>                            │

└────────────┬────────────────────────────────────┬────────────────────┘

&#x20;            │                                    │

&#x20;            ▼                                    ▼

┌───────────────────────────┐         ┌──────────────────────────────┐

│      LearningEngine       │         │           Database           │

│  (virtual thread)         │         │  (SQLite, prepared stmts)    │

│                           │         │                              │

│  loop:                    │  reads  │  topics, sources,            │

│   1. pick question        │────────▶│  knowledge, concepts,        │

│   2. search               │◀────────│  relationships, questions,   │

│   3. fetch content        │  writes │  gaps, contradictions,       │

│   4. KnowledgeExtractor   │         │  events, sessions            │

│   5. KnowledgeGapDetector │         └──────────────────────────────┘

│   6. QuestionGenerator    │

│   7. ContradictionDetector│

│                           │

└────┬─────────────────┬────┘

&#x20;    │                 │

&#x20;    ▼                 ▼

┌──────────────┐  ┌──────────────┐

│  AIProvider  │  │SearchProvider│

├──────────────┤  ├──────────────┤

│ OpenAI-      │  │ DuckDuckGo   │

│ Compatible   │  │ Custom       │

│              │  │              │

│ HttpClient   │  │ HttpClient   │

│ (async)      │  │ (async)      │

└──────────────┘  └──────────────┘

&#x20;       │                 │

&#x20;       ▼                 ▼

&#x20;  ┌──────────────────────────┐

&#x20;  │     External Services    │

&#x20;  │  • LLM endpoint (HTTPS)  │

&#x20;  │  • Search endpoint       │

&#x20;  │  • Source websites       │

&#x20;  └──────────────────────────┘

```



\## Threading Model



| Concern | Thread |

|---|---|

| UI rendering, property mutation | JavaFX Application Thread |

| Learning loop | Single virtual thread (`Thread.ofVirtual()`) |

| HTTP (AI, search, source fetch) | Java HttpClient internal (async) + virtual thread waiters |

| Export I/O | Single virtual thread per export |

| SQLite reads/writes | Called from whichever thread needs it; SQLite serializes internally |



\## Data Flow (one cycle)



```

ResearchQuestion

&#x20;    │

&#x20;    ├─▶ SearchProvider.search(q) ──▶ List<SearchResult>

&#x20;    │

&#x20;    ├─▶ for each new URL:

&#x20;    │       HttpClient GET

&#x20;    │       htmlToText

&#x20;    │       sources row (status=PROCESSED, content=...)

&#x20;    │       KnowledgeExtractor.extract → ExtractionResult

&#x20;    │       knowledge row + concepts + relationships

&#x20;    │       ContradictionDetector.scan → contradictions rows

&#x20;    │

&#x20;    ├─▶ synthesizeAnswer(question) using recent knowledge

&#x20;    │       questions.answer, questions.status=ANSWERED

&#x20;    │

&#x20;    └─▶ next cycle

```



\## Extensibility Points



\- \*\*AI providers\*\*: implement `ai.AIProvider`, register in `AIProviderFactory.from`.

\- \*\*Search providers\*\*: implement `search.SearchProvider`, register in `SearchProviderFactory.from`.

\- \*\*Extractors\*\*: extend `learning.KnowledgeExtractor` or add a parallel class and call it from `LearningEngine.runLoop`.

\- \*\*Detectors\*\*: mirror `ContradictionDetector`'s pattern (heuristic gate → AI call → persist).

