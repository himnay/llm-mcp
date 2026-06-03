CREATE TABLE employee (
      id BIGSERIAL PRIMARY KEY,
      username VARCHAR(255) UNIQUE NOT NULL,
      full_name VARCHAR(255),
      team VARCHAR(100) NOT NULL
);

CREATE TABLE leave_record (
      id BIGSERIAL PRIMARY KEY,
      username VARCHAR(255) NOT NULL,
      leave_date DATE NOT NULL,
      status VARCHAR(50) NOT NULL,
      created_at TIMESTAMP NOT NULL
);