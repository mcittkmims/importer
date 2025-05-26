CREATE TABLE IF NOT EXISTS company_industry (
    id         BIGSERIAL PRIMARY KEY,
    corporate_number VARCHAR(20) REFERENCES company(corporate_number),
    industry_code VARCHAR(10) REFERENCES industry(code),
    CONSTRAINT uq_company_product UNIQUE (corporate_number, industry_code)
);