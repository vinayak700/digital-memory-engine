# 🧠 Digital Memory Engine

> A high-performance, intelligence-first backend system for semantic memory recall and knowledge graph management.

The **Digital Memory Engine** is a production-grade Java/Spring Boot system designed to function as a "Second Brain". It ingests raw user memories, enhances them with AI intelligence (Embeddings, Relationship Graphs), and provides sub-second semantic answers to complex user questions.

---

## 🚀 Key Features

### 1. 🔍 Semantic Memory (Vector Search)
*   **Hybrid Search**: Combines Full-Text Search (PostgreSQL GIN) with Vector Similarity Search (1536-dim embeddings).
*   **Performance**: Optimized GIN Key search reduces complexity from $O(N)$ to $O(\log N)$.

### 2. 🕸️ Graph Memory (Relationships)
*   **Knowledge Graph**: Automatically links memories (e.g., "Event A *CAUSED* Event B").
*   **Optimized Traversal**: Solves N+1 query bottlenecks using efficient batch-fetching algorithms.

### 3. ⚡ Intelligent Caching
*   **Semantic Cache**: Understands that "Who is my wife?" and "What is my spouse's name?" are the same question.
*   **Inverted Index**: Uses a custom Redis Inverted Index to reduce lookup time from linear $O(N)$ to constant $O(K)$.

### 4. 🌊 Event-Driven Architecture
*   **Async Processing**: Uses **Apache Kafka** to decouple high-latency tasks (Embedding Generation) from user-facing APIs.
*   **Production Hardened**: Configured with **Concurrent Consumers** and **Poison Pill Protection** to handle scale and failures gracefully.

---

## 🛠️ Technology Stack

| Component | Technology | Role |
| :--- | :--- | :--- |
| **Language** | Java 17 | Core Logic & Safety |
| **Framework** | Spring Boot 4.0 | REST API & Dependency Injection |
| **Database** | PostgreSQL 17 (Supabase) | Persistence & JSONB Context |
| **Cache (L1)** | Caffeine | In-Memory Micro-second Access |
| **Cache (L2)** | Redis (Upstash) | Distributed Semantic Cache |
| **Event Bus** | Apache Kafka (Confluent) | Async Decoupling & Reliability |
| **AI Model** | Google Gemini 1.5 | Reasoning & Answer Synthesis |
| **Migrations** | Flyway | Schema Version Control |

---

## 🏛️ System Architecture

> For detailed design, see [System Architecture](./.gemini/antigravity/brain/7f53da73-d445-4539-81dc-9a9a0b69609e/system_architecture.md).

### High-Level Components
1.  **Orchestrator**: `AnswerSynthesisEngine` coordinates retrieval and reasoning.
2.  **Intelligence Layer**: `GeminiService` and `EmbeddingService` provide the "brains".
3.  **Data Layer**: `MemoryRepository` and `RelationshipRepository` manage state.

### Data Model
> For schema details, see [Data Architecture (ERD)](./.gemini/antigravity/brain/7f53da73-d445-4539-81dc-9a9a0b69609e/data_architecture.md).

---

## 🚦 Getting Started

### Prerequisites
*   Java 17+
*   Maven 3.8+
*   Environment Variables (See `application.properties`):
    *   `GEMINI_API_KEY`
    *   `SPRING_DATASOURCE_URL`
    *   `SPRING_KAFKA_BOOTSTRAP_SERVERS`
    *   `SPRING_DATA_REDIS_HOST`

### Run Locally
```bash
# Clean, Build, and Run
./mvnw clean spring-boot:run
```

### Run Tests
```bash
# Run Unit and Integration Tests
./mvnw clean test
```

---

## 🛡️ Production Readiness (Recent Fixes)

This engine has been audited and fixed for production scale:

*   ✅ **Security**: Credentials moved to environment variables.
*   ✅ **Search Perf**: PostgreSQL GIN Index added (Migration V2).
*   ✅ **Graph Perf**: Recursive N+1 Graph queries optimized to Batch queries.
*   ✅ **Safety**: Blocking I/O replaced with Resilient Backoff strategies.
*   ✅ **Reliability**: Kafka consumers scaled to 3x concurrency with Dead Letter logic.

---

## 🧑‍💻 Author

Built as a **Production-Grade System Design Project**, demonstrating advanced patterns in Distributed Systems, AI Integration, and Java Concurrency.
