CREATE TABLE IF NOT EXISTS comparisons (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    request_id      UUID NOT NULL REFERENCES requests(id) ON DELETE CASCADE,
    match_status    VARCHAR(20) NOT NULL CHECK (match_status IN ('MATCH', 'MISMATCH', 'ERROR', 'TIMEOUT')),
    latency_diff    BIGINT,
    v1_status_code  INTEGER,
    v2_status_code  INTEGER,
    diff_details    JSONB,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comparisons_request_id ON comparisons(request_id);
CREATE INDEX idx_comparisons_match_status ON comparisons(match_status);
CREATE INDEX idx_comparisons_created_at ON comparisons(created_at DESC);
