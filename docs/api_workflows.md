# API Workflows

This document explains the logical flow of the key APIs in the Digital Memory Engine.

## 1. Memory Creation Workflow
**Endpoint**: `POST /api/v1/memories`

**Goal**: Ingest a new thought/memory and asynchronously enrich it.

1.  **Validation**: Check if content is not empty.
2.  **Persistence**: Save the raw text to Supabase immediately.
3.  **Event Emission**: Publish a `MemoryCreatedEvent` to Kafka. This decouples the heavy AI processing from the user response.
4.  **Response**: Return `201 Created` with the Memory ID immediately to the user.
5.  **Background Processing (Async)**:
    *   **Embedding**: Convert text to vector using Gemini -> Update DB.
    *   **Topic Extraction**: Extract tags -> Save to `topics` table.
    *   **Intelligent Linking**: a. Search for existing similar memories using the new vector. b. Ask Gemini: "Is this new memory related to this existing one?" c. If YES -> Create a `MemoryRelationship` in the DB.

## 2. Intelligent Ask (RAG) Workflow
**Endpoint**: `POST /api/v1/ask`

**Goal**: Answer a user's natural language question using their stored memories.

1.  **Cache Check**: Generate an embedding for the *question*. Check Redis "Semantic Cache". If a similar question was asked recently (similarity > 0.9), return the cached answer instantly.
2.  **Intent Understanding**: Use Gemini to extract "Search Terms" from the question (e.g., "What did I do last week?" -> Keywords: "work, meeting, gym").
3.  **Hybrid Retrieval**:
    *   **Vector Search**: Find memories conceptually similar to the question.
    *   **Keyword Search**: Find memories containing specific extracted keywords.
    *   **Rank & Merge**: Combine results and remove duplicates.
4.  **Context Expansion (The Graph)**: For the top retrieved memories, fetch their *connected* neighbors from the `memory_relationships` table (Graph Traversal).
5.  **Synthesis**:
    *   Construct a prompt: "Here are the user's memories: [Text 1, Text 2...]. Answer the question: [Question]."
    *   Send to Gemini.
6.  **Response**: Return the generated answer + list of "Source" memories used.

## 3. Search Workflow
**Endpoint**: `POST /api/v1/search`

1.  **Input**: User sends a query (e.g., "Kafka config").
2.  **Strategy Selection**:
    *   If query is short/exact -> Use SQL `ILIKE`.
    *   If query is complex -> Use Vector Similarity (`<=>` operator in Postgres).
3.  **Execution**: Run the query against `memories` table.
4.  **Result**: Return top matches sorted by relevance score.

## 4. Relationship Management
**Endpoint**: `GET /api/v1/relationships/memory/{id}`

1.  **Fetch**: Query `memory_relationships` table for rows where `source_id` OR `target_id` matches the input ID.
2.  **Map**: Normalize the result so the user sees a list of "Related Memory" objects, regardless of direction.
