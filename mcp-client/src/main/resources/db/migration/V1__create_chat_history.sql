CREATE TABLE IF NOT EXISTS chat_message (
    id           BIGSERIAL    PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    message_type VARCHAR(20)  NOT NULL,
    content      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_message_conv_time
    ON chat_message (conversation_id, created_at ASC);
