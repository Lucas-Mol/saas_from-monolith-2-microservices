-- Single-tenant schema: one database per customer instance, no tenant column by design.

CREATE TABLE customers (
    id          BIGSERIAL PRIMARY KEY,
    full_name   VARCHAR(150) NOT NULL,
    email       VARCHAR(150),
    phone       VARCHAR(30),
    birth_date  DATE,
    notes       TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE professionals (
    id             BIGSERIAL PRIMARY KEY,
    full_name      VARCHAR(150) NOT NULL,
    specialty      VARCHAR(100),
    work_start     TIME NOT NULL,
    work_end       TIME NOT NULL,
    session_price  NUMERIC(10, 2) NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(150) NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    full_name      VARCHAR(150) NOT NULL,
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'STAFF', 'CUSTOMER')),
    customer_id    BIGINT REFERENCES customers (id),
    enabled        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE appointments (
    id               BIGSERIAL PRIMARY KEY,
    customer_id      BIGINT NOT NULL REFERENCES customers (id),
    professional_id  BIGINT NOT NULL REFERENCES professionals (id),
    start_time       TIMESTAMP NOT NULL,
    end_time         TIMESTAMP NOT NULL,
    status           VARCHAR(20) NOT NULL,
    notes            TEXT,
    cancel_reason    TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_appointments_professional_start ON appointments (professional_id, start_time);
CREATE INDEX idx_appointments_customer ON appointments (customer_id);

CREATE TABLE invoices (
    id           BIGSERIAL PRIMARY KEY,
    customer_id  BIGINT NOT NULL REFERENCES customers (id),
    status       VARCHAR(20) NOT NULL,
    total        NUMERIC(10, 2) NOT NULL,
    issued_at    TIMESTAMP NOT NULL,
    paid_at      TIMESTAMP
);

CREATE TABLE invoice_lines (
    id              BIGSERIAL PRIMARY KEY,
    invoice_id      BIGINT NOT NULL REFERENCES invoices (id),
    appointment_id  BIGINT NOT NULL UNIQUE REFERENCES appointments (id),
    description     VARCHAR(255) NOT NULL,
    amount          NUMERIC(10, 2) NOT NULL
);
