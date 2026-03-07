CREATE TABLE notification (
      id BIGSERIAL PRIMARY KEY,
      channel VARCHAR(50) NOT NULL,
      recipient VARCHAR(255) NOT NULL,
      message TEXT NOT NULL,
      created_at TIMESTAMP NOT NULL
);