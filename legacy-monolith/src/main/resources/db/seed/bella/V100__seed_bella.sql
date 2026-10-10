-- Demo seed for the "Bella Salon" instance. Dev-only password for every user: dev-password
-- Appointments are relative to CURRENT_DATE so the demo always has an agenda for today.

INSERT INTO customers (full_name, email, phone, birth_date, notes) VALUES
    ('Fernanda Lima', 'fernanda@example.test', '+55 21 90000-0011', '1995-02-18', 'Allergic to ammonia'),
    ('Gabriel Souza', 'gabriel@example.test',  '+55 21 90000-0012', NULL,         NULL);

INSERT INTO professionals (full_name, specialty, work_start, work_end, session_price) VALUES
    ('Helena Dias', 'Hairdresser', '09:00', '18:00', 120.00),
    ('Igor Matos',  'Manicure',    '11:00', '20:00',  60.00);

INSERT INTO users (email, password_hash, full_name, role, customer_id) VALUES
    ('admin@bella.test', '$2a$10$heLb6vIMeqDeAh9HautSqeEZxkslHAjYfVD5uQbhH8/.WnmgBRw.u', 'Bella Admin', 'ADMIN', NULL),
    ('staff@bella.test', '$2a$10$heLb6vIMeqDeAh9HautSqeEZxkslHAjYfVD5uQbhH8/.WnmgBRw.u', 'Bella Reception', 'STAFF', NULL);

INSERT INTO appointments (customer_id, professional_id, start_time, end_time, status, notes) VALUES
    (1, 1, CURRENT_DATE + TIME '09:00', CURRENT_DATE + TIME '10:30', 'CONFIRMED', 'Haircut and coloring'),
    (2, 2, CURRENT_DATE + TIME '13:00', CURRENT_DATE + TIME '13:45', 'SCHEDULED', NULL),
    (2, 1, CURRENT_DATE - 2 + TIME '15:00', CURRENT_DATE - 2 + TIME '16:00', 'COMPLETED', NULL);
