-- Enable pgvector extension (Supabase has this pre-installed)
CREATE EXTENSION IF NOT EXISTS vector;

-- Add embedding column to memories table
ALTER TABLE memories 
ADD COLUMN IF NOT EXISTS embedding vector(1536);

-- Create index for similarity search (IVFFlat for large datasets)
CREATE INDEX IF NOT EXISTS idx_memories_embedding 
ON memories 
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- Add comment for documentation
COMMENT ON COLUMN memories.embedding IS 'Vector embedding for semantic similarity search (OpenAI ada-002 dimension: 1536)';
