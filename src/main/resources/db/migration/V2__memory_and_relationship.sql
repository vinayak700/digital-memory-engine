CREATE TABLE memories (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    context_json JSONB,
    importance_score INT,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ,
    archived BOOLEAN
);

CREATE INDEX idx_memory_user ON memories(user_id);
CREATE INDEX idx_memory_created ON memories(created_at);

CREATE TABLE memory_relationships (
    id BIGSERIAL PRIMARY KEY,
    from_memory_id BIGINT,
    to_memory_id BIGINT,
    type VARCHAR(255),
    weight INT,
    created_at TIMESTAMPTZ
);

CREATE INDEX idx_from_memory ON memory_relationships(from_memory_id);
CREATE INDEX idx_to_memory ON memory_relationships(to_memory_id);

CREATE TABLE memory_outcomes (
    id BIGSERIAL PRIMARY KEY,
    memory_id BIGINT,
    outcome_summary TEXT,
    satisfaction_score INT,
    recorded_at TIMESTAMPTZ
);
