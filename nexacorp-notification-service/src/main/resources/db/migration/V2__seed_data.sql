

INSERT INTO notification (channel, recipient, message, created_at)
VALUES
    ('INTERNAL', 'dev-team', 'Initial system boot notification', now());