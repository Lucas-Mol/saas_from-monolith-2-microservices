-- Deterministic dataset for characterization tests. The test clock is fixed at 2026-01-12T08:00 (Monday).
-- Password for every user: test-password
TRUNCATE invoice_lines, invoices, appointments, users, professionals, customers RESTART IDENTITY CASCADE;

INSERT INTO customers (full_name, email, phone, birth_date, notes, created_at) VALUES
    ('Ana Test',  'ana@test.local',  '+55 11 91111-0001', '1990-01-20', NULL,                    '2025-12-01 10:00:00'),
    ('Bob Test',  'bob@test.local',  '+55 11 91111-0002', NULL,         'Prefers morning slots', '2025-12-02 11:00:00'),
    ('Cris Test', 'cris@test.local', NULL,                NULL,         NULL,                    '2025-12-03 12:00:00');

INSERT INTO professionals (full_name, specialty, work_start, work_end, session_price, active) VALUES
    ('Dr. Paula Test', 'Physiotherapy', '08:00', '17:00', 150.00, TRUE),
    ('Dr. Rui Test',   'Nutrition',     '10:00', '18:00', 200.00, TRUE),
    ('Ivo Inactive',   'Massage',       '08:00', '12:00',  90.00, FALSE);

INSERT INTO users (email, password_hash, full_name, role, customer_id) VALUES
    ('admin@test.local',    '$2a$10$1FGjUgMsbJsVJmJzFsb/3.0ClxLoCK8Um5yd/UMEo163/3NBkLF.K', 'Test Admin', 'ADMIN',    NULL),
    ('staff@test.local',    '$2a$10$1FGjUgMsbJsVJmJzFsb/3.0ClxLoCK8Um5yd/UMEo163/3NBkLF.K', 'Test Staff', 'STAFF',    NULL),
    ('ana.user@test.local', '$2a$10$1FGjUgMsbJsVJmJzFsb/3.0ClxLoCK8Um5yd/UMEo163/3NBkLF.K', 'Ana Test',   'CUSTOMER', 1),
    ('orphan@test.local',   '$2a$10$1FGjUgMsbJsVJmJzFsb/3.0ClxLoCK8Um5yd/UMEo163/3NBkLF.K', 'No Record',  'CUSTOMER', NULL);

INSERT INTO appointments (customer_id, professional_id, start_time, end_time, status, notes, cancel_reason, created_at) VALUES
    (1, 1, '2026-01-12 09:00', '2026-01-12 10:00', 'CONFIRMED', NULL,          NULL,            '2026-01-02 09:00'),
    (2, 1, '2026-01-12 10:00', '2026-01-12 11:00', 'SCHEDULED', 'First visit', NULL,            '2026-01-03 09:00'),
    (3, 2, '2026-01-12 14:00', '2026-01-12 15:00', 'SCHEDULED', NULL,          NULL,            '2026-01-04 09:00'),
    (1, 2, '2026-01-05 11:00', '2026-01-05 12:00', 'COMPLETED', NULL,          NULL,            '2025-12-20 09:00'),
    (2, 1, '2026-01-09 08:00', '2026-01-09 09:00', 'COMPLETED', NULL,          NULL,            '2025-12-21 09:00'),
    (1, 1, '2026-01-15 16:00', '2026-01-15 17:00', 'SCHEDULED', NULL,          NULL,            '2026-01-05 09:00'),
    (2, 2, '2026-01-13 10:00', '2026-01-13 11:00', 'CANCELLED', NULL,          'Customer sick', '2026-01-06 09:00');

INSERT INTO invoices (customer_id, status, total, issued_at, paid_at) VALUES
    (1, 'PAID', 200.00, '2026-01-06 09:00', '2026-01-07 15:30');

INSERT INTO invoice_lines (invoice_id, appointment_id, description, amount) VALUES
    (1, 4, 'Nutrition session with Dr. Rui Test on 2026-01-05', 200.00);
