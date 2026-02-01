# API Reference (CURL Samples)

Use these commands to interact with your deployed Digital Memory Engine.

## Base URL
- **Production**: `https://digital-memory-engine.onrender.com/api/v1`
- **Local**: `http://localhost:8082/api/v1`

---

## 1. Memories

### Create a Memory
```bash
curl -X POST "https://digital-memory-engine.onrender.com/api/v1/memories" \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic [YOUR_AUTH_HEADER]" \
  -d '{
    "content": "Learned about Kafka partitions today. They allow parallel processing within a topic."
  }'
```

### Get All Memories (Paginated)
```bash
curl "https://digital-memory-engine.onrender.com/api/v1/memories?page=0&size=10" \
  -H "Authorization: Basic [YOUR_AUTH_HEADER]"
```

### Get Single Memory
```bash
curl "https://digital-memory-engine.onrender.com/api/v1/memories/1" \
  -H "Authorization: Basic [YOUR_AUTH_HEADER]"
```

### Archive (Soft Delete) Memory
```bash
curl -X DELETE "https://digital-memory-engine.onrender.com/api/v1/memories/1" \
  -H "Authorization: Basic [YOUR_AUTH_HEADER]"
```

---

## 2. Intelligence & Search

### Ask a Question (RAG)
```bash
curl -X POST "https://digital-memory-engine.onrender.com/api/v1/ask" \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic [YOUR_AUTH_HEADER]" \
  -d '{
    "question": "What have I learned about distributed systems?"
  }'
```

### Semantic Search
```bash
curl -X POST "https://digital-memory-engine.onrender.com/api/v1/search" \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic [YOUR_AUTH_HEADER]" \
  -d '{
    "query": "kafka config",
    "limit": 5
  }'
```

---

## 3. Graph Relationships

### Get Related Memories
```bash
curl "https://digital-memory-engine.onrender.com/api/v1/relationships/memory/1" \
  -H "Authorization: Basic [YOUR_AUTH_HEADER]"
```

---

## 4. Monitoring (Health)

### Check Health Status
```bash
curl "https://digital-memory-engine.onrender.com/actuator/health"
```
