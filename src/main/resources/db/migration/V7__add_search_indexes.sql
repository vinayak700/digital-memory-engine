-- Add GIN index to optimize full-text search queries
-- This changes search performance from O(N) to O(log N)

CREATE INDEX IF NOT EXISTS idx_memories_search_vector 
ON public.memories 
USING GIN (to_tsvector('english', COALESCE(title, '') || ' ' || COALESCE(content, '')));
