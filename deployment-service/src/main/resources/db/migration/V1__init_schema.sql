CREATE TABLE deployment (
        id BIGSERIAL PRIMARY KEY,
        service_name VARCHAR(255) NOT NULL,
        environment VARCHAR(50) NOT NULL,
        scheduled_time TIMESTAMP NOT NULL,
        status VARCHAR(50) NOT NULL,
        owner VARCHAR(255),
        created_at TIMESTAMP NOT NULL,
        updated_at TIMESTAMP NOT NULL
);