# Technology Stack & Glossary

## 1. Core Stack

### **Spring Boot (Java)**
The framework used for the Backend API. Chosen for its robustness, enterprise-grade security, and massive ecosystem for building microservices.

### **PostgreSQL + pgvector**
- **PostgreSQL**: The primary relational database. Stores user data, text, and relationships.
- **pgvector**: An extension for Postgres that allows storing "Vector Embeddings" (arrays of numbers representing meaning). This enables "Semantic Search" (finding similar concepts, not just matching words).

### **React (Vite) + TypeScript**
The frontend framework.
- **React**: Component-based UI library.
- **Vite**: Extremely fast build tool.
- **TypeScript**: Adds strong typing to JavaScript, preventing bugs at compile time.

### **Upstash Redis**
A serverless, in-memory key-value store.
- **Usage**: Used for **Semantic Caching**. It stores the valid answers to questions so we don't need to re-query the AI if the same question is asked twice.

## 2. Advanced Components

### **Apache Kafka (Confluent Cloud)**
A distributed event streaming platform.
- **Usage**: Decouples the API from background tasks. When you save a memory, the API returns instantly. Kafka holds the "Event" (`MemoryCreatedEvent`), and a background worker picks it up later to do the slow work (generating embeddings, AI analysis). This makes the interface feel snappy.

### **Google Gemini 1.5 Flash**
The Large Language Model (LLM).
- **Usage**:
    1.  **Embeddings**: Converts text into vectors.
    2.  **Synthesis**: Reads your memories and answers your questions.
    3.  **Reasoning**: Decides if two memories are related.

### **Supabase**
A hosted backend-as-a-service that provides the Postgres database.
- **Session Pooler**: A way to manage database connections (Port 5432).
- **Transaction Pooler**: A scalable way to manage thousands of connections (Port 6543) - **Used in Production**.

## 3. Key Concepts / Methodologies

### **RAG (Retrieval-Augmented Generation)**
A technique where we don't just ask the AI a question from its training data. Instead, we:
1.  **Retrieve**: Search *your* database for relevant private facts.
2.  **Augment**: Paste those facts into the prompt.
3.  **Generate**: Ask the AI to answer using *only* that data.

### **Event-Driven Architecture (EDA)**
A design pattern where components communicate by sending "Events" (messages) rather than calling each other directly.
- *Benefit*: If the AI service goes down, your app still works (you can still save memories), and the AI work catches up later.

### **Semantic Search**
Searching by *meaning* rather than keywords.
- *Example*: Searching for "canine" will find "dog", even if the word "canine" isn't in the text.

### **Semantic Caching**
Smart caching. If User A asks "What is Java?" and later asks "Explain Java to me?", the system recognizes these are 95% similar and returns the cached answer for the first question, saving money and time.
