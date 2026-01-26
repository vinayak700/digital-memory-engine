# Manual Testing Curl Commands

Run these commands to verify the API functionality.
Ensure the application is running on `http://localhost:8082`.

## 1. System Check

**Health Check (JSON)**
```bash
curl -X GET http://localhost:8082/api/test | json_pp
```

**Home Page (HTML)**
```bash
curl -X GET http://localhost:8082/
```

---

## 2. Memory Operations

**Create a Memory**
```bash
curl -X POST http://localhost:8082/api/v1/memories \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin" \
  -d '{
    "title": "Learning Spring Boot",
    "content": "Spring Boot makes it easy to create stand-alone, production-grade Spring based Applications. It provides opinionated starter dependencies to simplify your build configuration.",
    "importanceScore": 8
  }' | json_pp
```

**Create Another Memory (for linking)**
```bash
curl -X POST http://localhost:8082/api/v1/memories \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin" \
  -d '{
    "title": "Java Streams API",
    "content": "The Stream API is used to process collections of objects. A stream is a sequence of objects that supports various methods.",
    "importanceScore": 6
  }' | json_pp
```

**List Memories**
```bash
curl -X GET "http://localhost:8082/api/v1/memories?page=0&size=10" \
  -H "X-User-Id: admin" | json_pp
```

**Get Single Memory** (Replace `1` with actual ID)
```bash
curl -X GET http://localhost:8082/api/v1/memories/1 \
  -H "X-User-Id: admin" | json_pp
```

**Update Memory**
```bash
curl -X PATCH http://localhost:8082/api/v1/memories/1 \
  -H "Content-Type: application/json" \
  -H "X-User-Id: admin" \
  -d '{
    "content": "Updated: Spring Boot is awesome for microservices!",
    "importanceScore": 9
  }' | json_pp
```

**Delete (Archive) Memory**
```bash
curl -X DELETE http://localhost:8082/api/v1/memories/1 \
  -H "X-User-Id: admin"
```

---

## 3. Relationship Operations

**Create Relationship**
(Link Memory 1 to Memory 2. Adjust IDs accordingly.)
```bash
curl -X POST http://localhost:8082/api/v1/relationships \
  -H "Content-Type: application/json" \
  -d '{
    "sourceMemoryId": 1,
    "targetMemoryId": 2,
    "type": "RELATED_TO",
    "strength": 0.8
  }'
```

**Get Related Memories**
```bash
curl -X GET http://localhost:8082/api/v1/relationships/memory/1 | json_pp
```

**Traverse Relationship Graph**
```bash
curl -X GET "http://localhost:8082/api/v1/relationships/memory/1/traverse?depth=2" | json_pp
```

**Delete Relationship** (Replace `1` with actual relationship ID)
```bash
curl -X DELETE http://localhost:8082/api/v1/relationships/1
```

---

## 4. Search & Intelligence

**Semantic Search**
```bash
curl -X POST http://localhost:8082/api/v1/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "framework for backend",
    "limit": 5,
    "similarityThreshold": 0.5
  }' | json_pp
```

**Find Similar Memories**
```bash
curl -X GET "http://localhost:8082/api/v1/search/similar/1?limit=3" | json_pp
```

**Ask Question (POST)**
```bash
curl -X POST http://localhost:8082/api/v1/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is the best way to build microservices based on my notes?",
    "includeRelated": true,
    "maxSources": 3
  }' | json_pp
```

**Ask Question (GET)**
```bash
curl -X GET "http://localhost:8082/api/v1/ask?q=What+do+I+know+about+Java" | json_pp
```
