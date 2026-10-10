-- Demo seed for the "Acme Clinic" instance. Dev-only password for every user: dev-password
-- Appointments are relative to CURRENT_DATE so the demo always has an agenda for today.

INSERT INTO customers (full_name, email, phone, birth_date, notes) VALUES
    ('Alice Martins',  'alice@example.test',  '+55 11 90000-0001', '1990-04-12', NULL),
    ('Bruno Costa',    'bruno@example.test',  '+55 11 90000-0002', '1985-09-30', 'Prefers morning slots'),
    ('Carla Ferreira', 'carla@example.test',  '+55 11 90000-0003', NULL,         NULL);

INSERT INTO professionals (full_name, specialty, work_start, work_end, session_price) VALUES
    ('Dr. Daniel Rocha', 'Physiotherapy', '08:00', '17:00', 180.00),
    ('Dr. Elisa Prado',  'Nutrition',     '10:00', '19:00', 220.00);

INSERT INTO users (email, password_hash, full_name, role, customer_id) VALUES
    ('admin@acme.test',    '$2a$10$heLb6vIMeqDeAh9HautSqeEZxkslHAjYfVD5uQbhH8/.WnmgBRw.u', 'Acme Admin', 'ADMIN',    NULL),
    ('staff@acme.test',    '$2a$10$heLb6vIMeqDeAh9HautSqeEZxkslHAjYfVD5uQbhH8/.WnmgBRw.u', 'Acme Front Desk', 'STAFF', NULL),
    ('alice@example.test', '$2a$10$heLb6vIMeqDeAh9HautSqeEZxkslHAjYfVD5uQbhH8/.WnmgBRw.u', 'Alice Martins', 'CUSTOMER', 1);

INSERT INTO appointments (customer_id, professional_id, start_time, end_time, status, notes) VALUES
    (1, 1, CURRENT_DATE + TIME '09:00', CURRENT_DATE + TIME '10:00', 'CONFIRMED', NULL),
    (2, 1, CURRENT_DATE + TIME '10:00', CURRENT_DATE + TIME '11:00', 'SCHEDULED', 'First visit'),
    (3, 2, CURRENT_DATE + TIME '14:00', CURRENT_DATE + TIME '15:00', 'SCHEDULED', NULL),
    (1, 2, CURRENT_DATE - 7 + TIME '11:00', CURRENT_DATE - 7 + TIME '12:00', 'COMPLETED', NULL),
    (2, 1, CURRENT_DATE - 3 + TIME '08:00', CURRENT_DATE - 3 + TIME '09:00', 'COMPLETED', NULL),
    (3, 1, CURRENT_DATE + 1 + TIME '16:00', CURRENT_DATE + 1 + TIME '17:00', 'SCHEDULED', NULL);

INSERT INTO invoices (customer_id, status, total, issued_at, paid_at) VALUES
    (1, 'PAID', 220.00, CURRENT_DATE - 6 + TIME '09:00', CURRENT_DATE - 5 + TIME '15:30');

INSERT INTO invoice_lines (invoice_id, appointment_id, description, amount) VALUES
    (1, 4, 'Nutrition session with Dr. Elisa Prado', 220.00);
