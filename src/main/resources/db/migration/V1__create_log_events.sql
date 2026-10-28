CREATE TABLE log_events (
    event_id UUID PRIMARY KEY,
    org_id TEXT NOT NULL,
    service TEXT NOT NULL,
    level TEXT NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    message TEXT NOT NULL,
    trace_id TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_log_events_org_timestamp ON log_events (org_id, event_timestamp DESC);
CREATE INDEX idx_log_events_org_service_timestamp ON log_events (org_id, service, event_timestamp DESC);
CREATE INDEX idx_log_events_org_level_timestamp ON log_events (org_id, level, event_timestamp DESC);
