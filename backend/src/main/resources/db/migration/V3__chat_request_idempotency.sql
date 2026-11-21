
CREATE TABLE chat_request_idempotency (
    user_id UUID NOT NULL REFERENCES app_user(id),
    request_id UUID NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING'
        CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED')),
    response JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (user_id, request_id)
);
