-- Topics table for memory categorization
CREATE TABLE IF NOT EXISTS topics (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, name)
);

-- Memory-Topic relationship (many-to-many)
CREATE TABLE IF NOT EXISTS memory_topics (
    memory_id BIGINT NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    topic_id BIGINT NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    PRIMARY KEY (memory_id, topic_id)
);

-- Drop legacy memory_relationships table if it exists with old schema
DROP TABLE IF EXISTS memory_relationships CASCADE;

-- Memory-Memory relationships (recreated with correct schema)
CREATE TABLE memory_relationships (
    id BIGSERIAL PRIMARY KEY,
    source_memory_id BIGINT NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    target_memory_id BIGINT NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    relationship_type VARCHAR(50) NOT NULL,
    strength DECIMAL(3,2) DEFAULT 1.0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(source_memory_id, target_memory_id, relationship_type)
);

-- Indexes for graph traversal
CREATE INDEX IF NOT EXISTS idx_memory_topics_memory ON memory_topics(memory_id);
CREATE INDEX IF NOT EXISTS idx_memory_topics_topic ON memory_topics(topic_id);
CREATE INDEX idx_relationships_source ON memory_relationships(source_memory_id);
CREATE INDEX idx_relationships_target ON memory_relationships(target_memory_id);
CREATE INDEX idx_relationships_type ON memory_relationships(relationship_type);
