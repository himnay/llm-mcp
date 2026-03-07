INSERT INTO deployment (service_name, environment, scheduled_time, status, owner, created_at, updated_at)
VALUES
    ('orders-service', 'PROD', now() + interval '1 day', 'SCHEDULED', 'john.doe', now(), now()),
    ('payment-service', 'QA', now() + interval '2 hours', 'SCHEDULED', 'alice.smith', now(), now()),
    ('inventory-service', 'DEV', now() - interval '1 hour', 'IN_PROGRESS', 'bob.johnson', now(), now());