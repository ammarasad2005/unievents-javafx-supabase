-- UniEvents — Demo Seed Data (PostgreSQL / Supabase)
-- FAST-NUCES SDA | Section E | 23I-0716 · 25I-1022
-- Default password for all demo accounts: demo123
-- SHA-256('demo123') = d3ad9315b7be5dd53b31a273b3b3aba5defe700808305aa16a3062b76658a791

BEGIN;

-- 1. Users (1 Admin, 3 Organizers, 6 Students)
INSERT INTO users (user_id, full_name, email, password_hash, is_active, role) VALUES
  (1,  'Dr. Ayesha Khan', 'ayesha.khan@unievents.edu', 'd3ad9315b7be5dd53b31a273b3b3aba5defe700808305aa16a3062b76658a791', TRUE, 'ADMIN'),
  (2,  'Hamza Tariq',     'hamza.tariq@nuces.edu',     'd3ad9315b7be5dd53b31a273b3b3aba5defe700808305aa16a3062b76658a791', TRUE, 'CLUB_ORGANIZER'),
  (3,  'Sara Malik',      'sara.malik@nuces.edu',      'd3ad9315b7be5dd53b31a273b3b3aba5defe700808305aa16a3062b76658a791', TRUE, 'CLUB_ORGANIZER'),
  (4,  'Usman Iqbal',     'usman.iqbal@nuces.edu',     'd3ad9315b7be5dd53b31a273b3b3aba5defe700808305aa16a3062b76658a791', TRUE, 'CLUB_ORGANIZER'),
  (5,  'Ali Raza',        'ali.raza@nuces.edu',        'd3ad9315b7be5dd53b31a273b3b3aba5defe700808305aa16a3062b76658a791', TRUE, 'STUDENT'),
  (6,  'Fatima Noor',     'fatima.noor@nuces.edu',     'd3ad9315b7be5dd53b31a273b3b3aba5defe700808305aa16a3062b76658a791', TRUE, 'STUDENT'),
  (7,  'Bilal Ahmed',     'bilal.ahmed@nuces.edu',     'd3ad9315b7be5dd53b31a273b3b3aba5defe700808305aa16a3062b76658a791', TRUE, 'STUDENT'),
  (8,  'Zainab Sheikh',   'zainab.sheikh@nuces.edu',   'd3ad9315b7be5dd53b31a273b3b3aba5defe700808305aa16a3062b76658a791', TRUE, 'STUDENT'),
  (9,  'Ahmed Hassan',    'ahmed.hassan@nuces.edu',    'd3ad9315b7be5dd53b31a273b3b3aba5defe700808305aa16a3062b76658a791', TRUE, 'STUDENT'),
  (10, 'Hira Siddiqui',   'hira.siddiqui@nuces.edu',   'd3ad9315b7be5dd53b31a273b3b3aba5defe700808305aa16a3062b76658a791', TRUE, 'STUDENT')
ON CONFLICT (user_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('users', 'user_id'), GREATEST((SELECT MAX(user_id) FROM users), 10));

INSERT INTO admins (user_id, department) VALUES
  (1, 'Student Affairs')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO club_organizers (user_id, is_president) VALUES
  (2, TRUE),
  (3, TRUE),
  (4, FALSE)
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO students (user_id, roll_number) VALUES
  (5,  '23I-0001'),
  (6,  '23I-0002'),
  (7,  '24I-0003'),
  (8,  '24I-0004'),
  (9,  '22I-0005'),
  (10, '24I-0006')
ON CONFLICT (user_id) DO NOTHING;

-- 2. Clubs & Categories
INSERT INTO clubs (club_id, name, description, is_active, created_by) VALUES
  (1, 'ACM Society',    'ACM student chapter — tech events, bootcamps and hackathons.', TRUE, 1),
  (2, 'Sports Society', 'Campus sports leagues, tournaments and fitness events.',       TRUE, 1)
ON CONFLICT (club_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('clubs', 'club_id'), GREATEST((SELECT MAX(club_id) FROM clubs), 2));

INSERT INTO club_organizer_assignments (club_id, organizer_id) VALUES
  (1, 2),
  (2, 3)
ON CONFLICT DO NOTHING;

INSERT INTO categories (category_id, name) VALUES
  (1, 'Technology'),
  (2, 'Sports'),
  (3, 'Cultural')
ON CONFLICT (category_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('categories', 'category_id'), GREATEST((SELECT MAX(category_id) FROM categories), 3));

-- 3. Events (covering PUBLISHED with seats, PUBLISHED at capacity, COMPLETED, and DRAFT)
INSERT INTO events (event_id, club_id, category_id, title, description, start_date_time, end_date_time, venue, capacity, status, check_in_code) VALUES
  (1, 1, 1, 'AI & ML Bootcamp',             'Intensive bootcamp on neural networks, embeddings, and model evaluation.', TIMESTAMPTZ '2026-11-27 09:00:00+05', TIMESTAMPTZ '2026-11-27 13:00:00+05', 'Seminar Hall A',  2,   'PUBLISHED', 'ACM-BOOT-26'),
  (2, 1, 1, 'Tech Dev Hour',                'Weekly developer session on clean architecture, JavaFX, and PostgreSQL.', TIMESTAMPTZ '2026-11-26 14:00:00+05', TIMESTAMPTZ '2026-11-26 16:00:00+05', 'CS Lab 3',        50,  'PUBLISHED', 'ACM-DEVHOUR-26'),
  (3, 1, 1, 'Inter-University Hackathon',   '24-hour coding challenge across universities with industry mentors.',     TIMESTAMPTZ '2026-11-18 09:00:00+05', TIMESTAMPTZ '2026-11-19 01:00:00+05', 'Main Auditorium', 100, 'COMPLETED', 'ACM-HACK-26'),
  (4, 2, 2, 'Marathon 5K',                  'Annual campus 5K endurance run open to all departments.',                 TIMESTAMPTZ '2026-11-30 08:00:00+05', TIMESTAMPTZ '2026-11-30 11:00:00+05', 'Sports Ground',   150, 'PUBLISHED', 'SPD-MARATHON-26'),
  (5, 1, 3, 'Freshers Welcome Night',       'Cultural evening, alumni talks, and society showcase for new batch.',     TIMESTAMPTZ '2026-12-05 18:00:00+05', TIMESTAMPTZ '2026-12-05 22:00:00+05', 'Main Auditorium', 60,  'DRAFT',     'ACM-FRESH-26'),
  (6, 1, 1, 'Cloud Native Docker Workshop', 'Hands-on containerization lab covering Dockerfiles and PostgreSQL stacks.', TIMESTAMPTZ '2026-12-02 14:00:00+05', TIMESTAMPTZ '2026-12-02 17:00:00+05', 'Karakoram Lab 1', 2,   'PUBLISHED', 'ACM-DOCKER-26')
ON CONFLICT (event_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('events', 'event_id'), GREATEST((SELECT MAX(event_id) FROM events), 6));

-- 4. Registrations, Attendances, Reviews & Conflicts
INSERT INTO registrations (registration_id, event_id, student_id, status, waitlist_position) VALUES
  (1,  1, 5, 'REGISTERED', NULL),
  (2,  1, 6, 'REGISTERED', NULL),
  (3,  1, 7, 'WAITLISTED', 1),
  (4,  1, 8, 'WAITLISTED', 2),
  (5,  1, 9, 'REGISTERED', NULL),
  (6,  2, 5, 'REGISTERED', NULL),
  (7,  2, 6, 'REGISTERED', NULL),
  (8,  2, 7, 'REGISTERED', NULL),
  (9,  2, 8, 'REGISTERED', NULL),
  (10, 3, 5, 'ATTENDED',   NULL),
  (11, 3, 6, 'ATTENDED',   NULL),
  (12, 3, 7, 'REGISTERED', NULL),
  (13, 6, 6, 'REGISTERED', NULL),
  (14, 6, 7, 'REGISTERED', NULL),
  (15, 6, 8, 'WAITLISTED', 1)
ON CONFLICT (registration_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('registrations', 'registration_id'), GREATEST((SELECT MAX(registration_id) FROM registrations), 15));

INSERT INTO attendances (attendance_id, registration_id, method) VALUES
  (1, 10, 'CODE'),
  (2, 11, 'MANUAL')
ON CONFLICT (attendance_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('attendances', 'attendance_id'), GREATEST((SELECT MAX(attendance_id) FROM attendances), 2));

INSERT INTO reviews (review_id, event_id, student_id, rating, comment) VALUES
  (1, 3, 6, 5, 'Best hackathon of the semester — amazing mentors!')
ON CONFLICT (review_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('reviews', 'review_id'), GREATEST((SELECT MAX(review_id) FROM reviews), 1));

INSERT INTO registration_conflicts (conflict_id, event_id, type, status, reason) VALUES
  (1, 1, 'CAPACITY_BREACH', 'OPEN', 'Bulk-import migration inserted a third REGISTERED student beyond the 2-seat capacity.')
ON CONFLICT (conflict_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('registration_conflicts', 'conflict_id'), GREATEST((SELECT MAX(conflict_id) FROM registration_conflicts), 1));

INSERT INTO conflict_registrations (conflict_id, registration_id) VALUES
  (1, 5)
ON CONFLICT DO NOTHING;

COMMIT;
