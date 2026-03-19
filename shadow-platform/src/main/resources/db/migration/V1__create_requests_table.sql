CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS requests (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    endpoint    VARCHAR(500)  NOT NULL,
    method      VARCHAR(10)   NOT NULL DEFAULT 'POST',
    payload     JSONB,
    headers     JSONB,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_requests_created_at ON requests(created_at DESC);
