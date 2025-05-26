CREATE TABLE IF NOT EXISTS company (
    id BIGSERIAL PRIMARY KEY,
    corporate_number VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50),
    update_date TIMESTAMP,
    location VARCHAR(255),
    postal_code VARCHAR(20),
    representative_name VARCHAR(255),
    representative_title VARCHAR(255),
    employee_count INTEGER,
    establishment_date DATE
);