CREATE SCHEMA IF NOT EXISTS dome_adapter;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS dome_adapter.procedure_retry (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    credential_id   UUID NOT NULL,
    action_type     VARCHAR(50)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    attempt_count   INTEGER      NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    first_failure_at TIMESTAMPTZ NOT NULL,
    payload         TEXT,

    CONSTRAINT uk_procedure_retry_credential_action
        UNIQUE (credential_id, action_type)
);

CREATE INDEX IF NOT EXISTS idx_procedure_retry_status
    ON dome_adapter.procedure_retry (status);

CREATE INDEX IF NOT EXISTS idx_procedure_retry_first_failure
    ON dome_adapter.procedure_retry (first_failure_at);

COMMENT ON TABLE  dome_adapter.procedure_retry IS 'Tracks retry attempts for label credential delivery to external response URIs';
COMMENT ON COLUMN dome_adapter.procedure_retry.credential_id   IS 'ID of the credential whose delivery is being retried';
COMMENT ON COLUMN dome_adapter.procedure_retry.action_type     IS 'Type of action: UPLOAD_LABEL_TO_RESPONSE_URI';
COMMENT ON COLUMN dome_adapter.procedure_retry.status          IS 'Retry status: PENDING, COMPLETED, RETRY_EXHAUSTED';
COMMENT ON COLUMN dome_adapter.procedure_retry.attempt_count   IS 'Number of scheduler-based retry attempts (not Reactor immediate retries)';
COMMENT ON COLUMN dome_adapter.procedure_retry.payload         IS 'JSON payload with data needed to reconstruct and re-execute the action';
