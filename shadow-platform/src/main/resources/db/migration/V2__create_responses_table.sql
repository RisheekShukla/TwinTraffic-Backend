CREATE TABLE IF NOT EXISTS responses (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    request_id      UUID NOT NULL REFERENCES requests(id) ON DELETE CASCADE,
    source          VARCHAR(10) NOT NULL CHECK (source IN ('v1', 'v2')),
    status_code     INTEGER,
    response_body   JSONB,
    latency_ms      BIGINT,
    error_message   TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_responses_request_id ON responses(request_id);
CREATE INDEX idx_responses_source ON responses(source);
