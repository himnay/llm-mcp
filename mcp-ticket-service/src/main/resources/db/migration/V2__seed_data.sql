INSERT INTO ticket (title, description, status, priority, assignee, created_at, updated_at)
VALUES
    ('Production API failure', 'Orders API returning 500 errors', 'OPEN', 'HIGH', 'john.doe', now(), now()),
    ('Payment gateway timeout', 'Stripe integration timing out intermittently', 'IN_PROGRESS', 'HIGH', 'alice.smith', now(), now()),
    ('UI dashboard bug', 'Incorrect revenue numbers displayed', 'OPEN', 'MEDIUM', 'bob.johnson', now(), now());