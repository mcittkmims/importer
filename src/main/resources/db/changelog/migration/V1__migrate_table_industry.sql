CREATE TABLE IF NOT EXISTS industry (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) UNIQUE NOT NULL,
    name VARCHAR(30),
    description VARCHAR(255)
);