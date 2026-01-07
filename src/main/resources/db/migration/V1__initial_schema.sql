CREATE TABLE test_flyway (
                             id BIGSERIAL PRIMARY KEY,
                             created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
