
---

## 🛠️ Tech Stack

### Core
- Java 17
- Spring Boot 4.x
- Spring Data JPA
- Hibernate ORM 7.x

### Database
- PostgreSQL 17 (Supabase)
- Session Pooler (production-safe connections)
- JSONB support for flexible context storage

### Schema Management
- Flyway
    - Versioned migrations
    - Schema locking
    - Validation on startup
    - Clean disabled for safety

### Infrastructure
- HikariCP (connection pooling)
- Apache Tomcat 11
- Spring Boot Actuator

---

## 🗄️ Database Schema (Initial)

### `memories`
Stores core memory records.

- `id` (PK)
- `user_id`
- `title`
- `content`
- `context_json` (JSONB)
- `importance_score`
- `archived`
- `created_at`
- `updated_at`

Indexes:
- `user_id`
- `created_at`

---

### `memory_outcomes`
Stores feedback or outcome metadata for memories.

- `id` (PK)
- `memory_id` (FK)
- `outcome_summary`
- `satisfaction_score`
- `recorded_at`

---

### `memory_relationships`
Models relationships between memories.

- `id` (PK)
- `from_memory_id`
- `to_memory_id`
- `type` (ENUM)
- `weight`
- `created_at`

Supported relationship types:
- `CAUSED_BY`
- `FOLLOWED_BY`
- `SUPPORTS`
- `CONTRADICTS`
- `REVISITS`

---

## 🔐 Schema Safety Decisions

✔ Hibernate auto-DDL disabled for production use  
✔ Flyway owns schema evolution  
✔ Baseline migrations enabled  
✔ Validation enforced on startup  
✔ No destructive operations allowed

This ensures:
- No accidental schema drift
- Predictable deployments
- Safe future migrations

---

## ⚙️ Configuration Highlights

- PostgreSQL via **Supabase Session Pooler**
- SSL enforced
- Connection pooling tuned
- Logging configured for Hibernate SQL visibility (dev)

Redis and Kafka configs exist but are **commented out intentionally**.

---

## 🧪 Current Runtime Behavior

- Application starts cleanly
- Flyway validates schema
- JPA initializes repositories
- Database connections are pooled and reused
- Tables are created only through migrations
- No runtime errors or port conflicts (after cleanup)

---

## 🧭 What’s Intentionally NOT Done Yet

- Redis caching logic
- Kafka producers / consumers
- REST APIs for memory operations
- Authentication & authorization
- Async workflows
- Search / ranking logic

These will be added **incrementally**, after the foundation is locked.

---

## 📌 Why This Approach

This project is built the way **real production systems** are built:
- Foundation first
- Schema safety over speed
- Explicit lifecycle management
- No “magic” auto-generation in production paths

---

## 🔮 Next Planned Steps

- Enable Flyway-only schema control (fully disable Hibernate DDL)
- Add REST APIs for memory CRUD
- Introduce Redis for hot-path caching
- Add Kafka for async memory events
- Implement JWT-based security
- Add observability (metrics + tracing)

---

## 🧑‍💻 Author

Built as a **system-design-focused backend project undertaking** to demonstrate production-grade engineering practices using Java and Spring Boot.
