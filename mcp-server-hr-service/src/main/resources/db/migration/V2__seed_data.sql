INSERT INTO employee (username, full_name, team)
VALUES
    ('himansu.nayak', 'John Doe', 'backend'),
    ('alice.smith', 'Alice Smith', 'backend'),
    ('sarah.dev', 'Sarah Dev', 'backend'),
    ('bob.johnson', 'Bob Johnson', 'frontend'),
    ('mike.ops', 'Mike Ops', 'devops');

-- Optional sample leave (for demo)
INSERT INTO leave_record (username, leave_date, status, created_at)
VALUES
    ('alice.smith', CURRENT_DATE + INTERVAL '1 day', 'APPROVED', now());