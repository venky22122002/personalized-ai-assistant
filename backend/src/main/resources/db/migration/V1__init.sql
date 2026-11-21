-- ============================================================
-- V1 — Initial schema for Personal AI Assistant
-- ============================================================

CREATE TABLE config_properties (
    id BIGSERIAL PRIMARY KEY,
    property_key VARCHAR(200) NOT NULL UNIQUE,
    property_value TEXT NOT NULL,
    value_type VARCHAR(30) NOT NULL DEFAULT 'STRING',
    description VARCHAR(500),
    is_sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE app_user (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    timezone      VARCHAR(80)  NOT NULL DEFAULT 'Asia/Kolkata',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------

CREATE TABLE conversations (
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES app_user(id),
    title      VARCHAR(200),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE chat_messages (
    id              UUID PRIMARY KEY,
    conversation_id UUID         NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content         TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_conversation_time
    ON chat_messages(conversation_id, created_at);

-- ----------------------------------------------------------------

CREATE TABLE memories (
    id         UUID         PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES app_user(id),
    category   VARCHAR(40)  NOT NULL,
    content    TEXT         NOT NULL,
    source     VARCHAR(40)  NOT NULL DEFAULT 'USER',
    importance SMALLINT     NOT NULL DEFAULT 3 CHECK (importance BETWEEN 1 AND 5),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_memories_user_category
    ON memories(user_id, category);

-- Full-text search index for memory retrieval
CREATE INDEX idx_memories_fts
    ON memories USING gin(to_tsvector('english', content));

-- ----------------------------------------------------------------

CREATE TABLE tasks (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES app_user(id),
    title       VARCHAR(250) NOT NULL,
    description TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'TODO'
                             CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED')),
    priority    VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM'
                             CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    due_at      TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_tasks_user_status
    ON tasks(user_id, status);

-- ----------------------------------------------------------------

CREATE TABLE pending_actions (
    id          UUID    PRIMARY KEY,
    user_id     UUID    NOT NULL REFERENCES app_user(id),
    action_type VARCHAR(60) NOT NULL,
    payload     JSONB       NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                CHECK (status IN ('PENDING','APPROVED','REJECTED','EXECUTED','FAILED','EXPIRED')),
    expires_at  TIMESTAMPTZ NOT NULL,
    approved_at TIMESTAMPTZ,
    executed_at TIMESTAMPTZ,
    result      TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_actions_user_status
    ON pending_actions(user_id, status);

-- ----------------------------------------------------------------

CREATE TABLE reminders (
    id         UUID         PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES app_user(id),
    task_id    UUID         REFERENCES tasks(id) ON DELETE SET NULL,
    title      VARCHAR(250) NOT NULL,
    remind_at  TIMESTAMPTZ  NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED'
               CHECK (status IN ('SCHEDULED', 'SENT', 'CANCELLED')),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_reminders_due
    ON reminders(remind_at)
    WHERE status = 'SCHEDULED';
