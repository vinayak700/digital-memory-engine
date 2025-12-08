-- Audit events table for compliance tracking
-- Append-only: events are never updated or deleted

CREATE TABLE IF NOT EXISTS audit_events (
    id BIGSERIAL PRIMARY KEY,
    memory_id BIGINT NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_data JSONB,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_audit_memory_id ON audit_events(memory_id);
CREATE INDEX IF NOT EXISTS idx_audit_user_id ON audit_events(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_event_type ON audit_events(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_occurred_at ON audit_events(occurred_at);

-- Add comment for documentation
COMMENT ON TABLE audit_events IS 'Append-only audit trail for memory operations';
