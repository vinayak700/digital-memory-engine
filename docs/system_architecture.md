# System Architecture

## 1. High-Level Design (HLD)
This diagram illustrates the infrastructure and high-level component interaction of the Digital Memory Engine.

```mermaid
graph TD
    User[User Device]
    
    subgraph Frontend [Frontend Vercel]
        UI[React SPA]
    end

    subgraph Backend [Backend Render]
        API[Spring Boot Engine]
        Security[Security Filter Chain]
        Controller[REST Controllers]
        Service[Service Layer]
    end

    subgraph Data [Data and Messaging]
        DB[(Supabase Postgres)]
        Vector[(pgvector Embeddings)]
        Redis[(Upstash Redis Cache)]
        Kafka{Confluent Kafka}
    end

    subgraph ExternalAI [External AI Services]
        Gemini[Google Gemini]
    end

    User -->|HTTPS| UI
    UI -->|REST API| API
    
    API --> Security
    Security --> Controller
    Controller --> Service
    
    Service -->|Read Write| DB
    Service -->|Search| Vector
    Service -->|Cache| Redis
    Service -->|Events| Kafka
    Service -->|Inference| Gemini

    Kafka -->|Consumer| Service
    
    DB -.-> Vector
```

## 2. Entity Relationship Diagram (ERD)
The database schema designed for graph-like relationships and vector search.

```mermaid
erDiagram
    USERS ||--o{ MEMORIES : owns
    USERS ||--o{ TOPICS : owns
    
    MEMORIES {
        bigint id PK
        uuid user_id FK
        text title
        text content
        vector embedding "1536 dims"
        int importance_score
        boolean archived
        timestamp created_at
    }

    MEMORY_RELATIONSHIPS {
        bigint id PK
        bigint source_memory_id FK
        bigint target_memory_id FK
        varchar type "CAUSES, RELATES_TO, etc"
        decimal strength
    }

    TOPICS {
        bigint id PK
        varchar name
    }

    MEMORIES ||--o{ MEMORY_RELATIONSHIPS : "source"
    MEMORIES ||--o{ MEMORY_RELATIONSHIPS : "target"
    MEMORIES }|--|{ TOPICS : "categorized_as"
```

## 3. Write Path (Memory Ingestion)
How data flows when a memory is created (**Event-Driven Architecture**).

```mermaid
sequenceDiagram
    participant User
    participant API as API Layer
    participant DB as Postgres
    participant Kafka as Kafka Topic
    participant Async as Async Consumer
    participant AI as Gemini AI
    participant Vector as pgvector

    User->>API: POST /memories
    API->>DB: Save Raw Memory
    API->>Kafka: Publish MemoryCreatedEvent
    API-->>User: 201 Created (Immediate Response)
    
    par Async Processing
        Kafka->>Async: Consume Event
        Async->>AI: Generate Embedding
        AI-->>Async: Vector[1536]
        Async->>Vector: Update Memory with Embedding
    and Keyword Extraction
        Async->>AI: Extract Keywords & Topics
        Async->>DB: Link Topics
    and Graph Linking
        Async->>Vector: Find Semantically Similar Memories
        Async->>AI: "Should these be linked?"
        AI-->>Async: YES/NO
        Async->>DB: Create Relationship (if YES)
    end
```

## 4. Read Path (Intelligent Retrieval / RAG)
How the system answers questions (**Retrieval-Augmented Generation**).

```mermaid
sequenceDiagram
    participant User
    participant Ask as AskController
    participant IQ as AnswerSynthesisEngine
    participant Redis as Semantic Cache
    participant Search as SearchService (Hybrid)
    participant Graph as GraphService
    participant AI as Gemini AI

    User->>Ask: POST /ask "What did I learn about Java?"
    Ask->>IQ: Process Question
    
    IQ->>Redis: Check Semantic Cache
    alt Cache Hit
        Redis-->>IQ: Return Cached Answer
        IQ-->>User: 200 OK (Instant)
    else Cache Miss
        IQ->>AI: Extract Search Keywords
        
        par Parallel Search
            IQ->>Search: Vector Search (Conceptual)
            IQ->>Search: Keyword Search (Exact Match)
        end
        
        Search-->>IQ: Returns List<ScoredMemory>
        
        IQ->>Graph: Expand Context (Get Related Memories)
        Graph-->>IQ: Returns Graph-Connected Memories
        
        IQ->>AI: Synthesize Answer (Prompt + Context)
        AI-->>IQ: Generated Natural Language Answer
        
        IQ->>Redis: Store in Cache
        IQ-->>User: 200 OK
    end
```

## 5. Component Interaction (Low-Level Design - LLD)
Detailing the internal modularity.

```mermaid
classDiagram
    class MemoryController {
        +createMemory()
        +getMemory()
    }
    class AskController {
        +ask()
    }
    class MemoryService {
        +save()
        +findById()
    }
    class SearchService {
        +search(query)
        +findSimilar()
    }
    class GraphService {
        +traverseGraph()
        +getRelated()
    }
    class GeminiService {
        +generateEmbedding()
        +generateAnswer()
    }

    MemoryController --> MemoryService
    AskController --> AnswerSynthesisEngine
    AnswerSynthesisEngine --> SearchService
    AnswerSynthesisEngine --> GraphService
    AnswerSynthesisEngine --> GeminiService
    
    SearchService --> JdbcTemplate : Reads
    MemoryService --> MemoryRepository : Writes
```
