-- Reset old demo student data so the three roll-numbered students stay consistent on every startup
WITH target_students AS (
    SELECT id
    FROM students
    WHERE email = 'sara.naveed@fast.edu'
       OR roll_number IN ('23I-0013', '23I-0661', '23I-0810')
)
DELETE FROM enrollments WHERE student_id IN (SELECT id FROM target_students);

WITH target_students AS (
    SELECT id
    FROM students
    WHERE email = 'sara.naveed@fast.edu'
       OR roll_number IN ('23I-0013', '23I-0661', '23I-0810')
)
DELETE FROM semester_progress WHERE student_id IN (SELECT id FROM target_students);

WITH target_students AS (
    SELECT id
    FROM students
    WHERE email = 'sara.naveed@fast.edu'
       OR roll_number IN ('23I-0013', '23I-0661', '23I-0810')
)
DELETE FROM attendance_records WHERE student_id IN (SELECT id FROM target_students);

WITH target_students AS (
    SELECT id
    FROM students
    WHERE email = 'sara.naveed@fast.edu'
       OR roll_number IN ('23I-0013', '23I-0661', '23I-0810')
)
DELETE FROM assessment_records WHERE student_id IN (SELECT id FROM target_students);

WITH target_students AS (
    SELECT id
    FROM students
    WHERE email = 'sara.naveed@fast.edu'
       OR roll_number IN ('23I-0013', '23I-0661', '23I-0810')
)
DELETE FROM fee_line_items WHERE challan_id IN (
    SELECT id FROM fee_challans WHERE student_id IN (SELECT id FROM target_students)
);

WITH target_students AS (
    SELECT id
    FROM students
    WHERE email = 'sara.naveed@fast.edu'
       OR roll_number IN ('23I-0013', '23I-0661', '23I-0810')
)
DELETE FROM fee_challans WHERE student_id IN (SELECT id FROM target_students);

WITH target_students AS (
    SELECT id
    FROM students
    WHERE email = 'sara.naveed@fast.edu'
       OR roll_number IN ('23I-0013', '23I-0661', '23I-0810')
)
DELETE FROM faculty_feedback_responses WHERE feedback_id IN (
    SELECT id FROM faculty_feedback WHERE student_id IN (SELECT id FROM target_students)
);

WITH target_students AS (
    SELECT id
    FROM students
    WHERE email = 'sara.naveed@fast.edu'
       OR roll_number IN ('23I-0013', '23I-0661', '23I-0810')
)
DELETE FROM faculty_feedback WHERE student_id IN (SELECT id FROM target_students);

WITH target_students AS (
    SELECT id
    FROM students
    WHERE email = 'sara.naveed@fast.edu'
       OR roll_number IN ('23I-0013', '23I-0661', '23I-0810')
)
DELETE FROM students WHERE id IN (SELECT id FROM target_students);

-- Seed base students
INSERT INTO students (full_name, roll_number, email, password, major, enrollment_year, gpa, current_semester)
VALUES
    ('Talha Qamar', '23I-0013', 'i230013@isb.nu.edu.pk', '12345678', 'Software Engineering', 2021, 3.48, 5),
    ('Syed Mustafa', '23I-0661', 'i230661@isb.nu.edu.pk', '12345678', 'Software Engineering', 2021, 3.42, 5),
    ('Muhammad Bin Taimur', '23I-0810', 'i230810@isb.nu.edu.pk', '12345678', 'Software Engineering', 2021, 3.36, 5)
ON CONFLICT (roll_number) DO UPDATE
SET full_name = EXCLUDED.full_name,
    email = EXCLUDED.email,
    password = EXCLUDED.password,
    major = EXCLUDED.major,
    enrollment_year = EXCLUDED.enrollment_year,
    gpa = EXCLUDED.gpa,
    current_semester = EXCLUDED.current_semester;

INSERT INTO faculty (full_name, email, password, department, designation)
VALUES
    ('Taaha Khan', 'tzk@nu.edu.pk', '12345678', 'Computer Science', 'Lecturer')
ON CONFLICT (email) DO NOTHING;

INSERT INTO admins (full_name, email, password)
VALUES ('Amir Rehman', 'amir.rehman@nu.edu.pk', '12345678')
ON CONFLICT (email) DO NOTHING;

-- Seed curriculum catalog
INSERT INTO courses (code, title, description, credit_hours, capacity, enrolled_count, term, semester_number, category, is_elective, is_lab)
VALUES
    ('NS1001', 'Applied Physics', 'Foundation course covering mechanics, EM, and waves.', 3, 60, 42, 'Semester 1', 1, 'CORE', FALSE, FALSE),
    ('MT1003', 'Calculus and Analytical Geometry', 'Differentiation, integration, and geometry.', 3, 60, 40, 'Semester 1', 1, 'CORE', FALSE, FALSE),
    ('SS1012', 'Functional English', 'Writing and comprehension skills.', 3, 60, 39, 'Semester 1', 1, 'CORE', FALSE, FALSE),
    ('SS1013', 'Ideology and Constitution of Pakistan', 'Civic foundations.', 3, 60, 38, 'Semester 1', 1, 'CORE', FALSE, FALSE),
    ('CL1000', 'Introduction to ICT', 'ICT fundamentals and labs.', 3, 60, 43, 'Semester 1', 1, 'CORE', FALSE, TRUE),
    ('CS1002', 'Programming Fundamentals', 'Problem solving using C/C++.', 3, 60, 45, 'Semester 1', 1, 'CORE', FALSE, TRUE),

    ('EE1005', 'Digital Logic Design', 'Combinational and sequential logic.', 3, 60, 44, 'Semester 2', 2, 'CORE', FALSE, TRUE),
    ('SS1014', 'Expository Writing', 'Critical writing.', 3, 60, 42, 'Semester 2', 2, 'CORE', FALSE, FALSE),
    ('SS1007', 'Islamic Studies/Ethics', 'Values and ethics.', 3, 60, 41, 'Semester 2', 2, 'CORE', FALSE, FALSE),
    ('MT1008', 'Multivariable Calculus', 'Advanced calculus.', 3, 60, 40, 'Semester 2', 2, 'CORE', FALSE, FALSE),
    ('CS1004', 'Object Oriented Programming', 'OOP with Java.', 3, 60, 46, 'Semester 2', 2, 'CORE', FALSE, TRUE),

    ('SS2043', 'Civics and Community Engagement', 'Service learning.', 3, 60, 37, 'Semester 3', 3, 'CORE', FALSE, FALSE),
    ('EE2003', 'Computer Organization and Assembly Language', 'CPU organization.', 3, 60, 39, 'Semester 3', 3, 'CORE', FALSE, TRUE),
    ('CS2001', 'Data Structures', 'Lists, trees, and graphs.', 3, 60, 44, 'Semester 3', 3, 'CORE', FALSE, TRUE),
    ('CS1005', 'Discrete Structures', 'Logic and combinatorics.', 3, 60, 42, 'Semester 3', 3, 'CORE', FALSE, FALSE),
    ('MT1004', 'Linear Algebra', 'Matrices and eigenvalues.', 3, 60, 41, 'Semester 3', 3, 'CORE', FALSE, FALSE),
    ('SSX21', 'Social Science Elective I', 'Elective slot.', 3, 40, 25, 'Semester 3', 3, 'ELECTIVE', TRUE, FALSE),

    ('CS2005', 'Database Systems', 'Relational modeling and SQL.', 3, 60, 47, 'Semester 4', 4, 'CORE', FALSE, TRUE),
    ('CS2006', 'Operating Systems', 'Processes, memory, and IO.', 3, 60, 45, 'Semester 4', 4, 'CORE', FALSE, TRUE),
    ('MT2005', 'Probability and Statistics', 'Probability theory.', 3, 60, 40, 'Semester 4', 4, 'CORE', FALSE, FALSE),
    ('SS2012', 'Technical and Business Writing', 'Professional communication.', 3, 60, 39, 'Semester 4', 4, 'CORE', FALSE, FALSE),
    ('SSX32', 'Social Science Elective II', 'Elective slot.', 3, 40, 24, 'Semester 4', 4, 'ELECTIVE', TRUE, FALSE),

    ('CS3001', 'Computer Networks', 'Routing and switching.', 3, 60, 32, 'Semester 5', 5, 'CORE', FALSE, TRUE),
    ('CS2009', 'Design and Analysis of Algorithms', 'Algorithmic strategies.', 3, 60, 34, 'Semester 5', 5, 'CORE', FALSE, FALSE),
    ('CS3005', 'Theory of Automata', 'Formal languages.', 3, 60, 33, 'Semester 5', 5, 'CORE', FALSE, FALSE),
    ('CSX01', 'CS Elective I', 'Open elective.', 3, 40, 20, 'Semester 5', 5, 'ELECTIVE', TRUE, FALSE),
    ('CSX02', 'CS Elective II', 'Open elective.', 3, 40, 18, 'Semester 5', 5, 'ELECTIVE', TRUE, FALSE),

    ('CS3014', 'Applied HCI', 'User experience studio.', 3, 60, 30, 'Semester 6', 6, 'CORE', FALSE, TRUE),
    ('AI2002', 'Artificial Intelligence', 'Classical AI.', 3, 60, 38, 'Semester 6', 6, 'CORE', FALSE, TRUE),
    ('CS4031', 'Compiler Construction', 'Front-end and back-end design.', 3, 60, 36, 'Semester 6', 6, 'CORE', FALSE, TRUE),
    ('EE3009', 'Computer Architecture', 'Pipeline design.', 3, 60, 34, 'Semester 6', 6, 'CORE', FALSE, TRUE),
    ('CS3009', 'Software Engineering', 'Process and quality.', 3, 60, 35, 'Semester 6', 6, 'CORE', FALSE, FALSE),
    ('CSX03', 'CS Elective III', 'Open elective.', 3, 40, 16, 'Semester 6', 6, 'ELECTIVE', TRUE, FALSE),

    ('CS4091', 'Final Year Project I', 'Capstone project kickoff.', 3, 40, 28, 'Semester 7', 7, 'FYP', FALSE, FALSE),
    ('CS3002', 'Information Security', 'Security principles.', 3, 60, 32, 'Semester 7', 7, 'CORE', FALSE, FALSE),
    ('CS3006', 'Parallel and Distributed Computing', 'Concurrency models.', 3, 60, 30, 'Semester 7', 7, 'CORE', FALSE, FALSE),
    ('CS4001', 'Professional Practices in IT', 'Ethics and law.', 3, 60, 29, 'Semester 7', 7, 'CORE', FALSE, FALSE),
    ('CSX04', 'CS Elective IV', 'Open elective.', 3, 40, 14, 'Semester 7', 7, 'ELECTIVE', TRUE, FALSE),

    ('CS4087', 'Advanced DBMS', 'Distributed databases.', 3, 60, 28, 'Semester 8', 8, 'CORE', FALSE, TRUE),
    ('MG4011', 'Entrepreneurship', 'Startup fundamentals.', 3, 60, 27, 'Semester 8', 8, 'CORE', FALSE, FALSE),
    ('CS4092', 'Final Year Project II', 'Capstone completion.', 6, 40, 20, 'Semester 8', 8, 'FYP', FALSE, FALSE),
    ('CSX05', 'CS Elective V', 'Open elective.', 3, 40, 12, 'Semester 8', 8, 'ELECTIVE', TRUE, FALSE),
    ('CSX06', 'CS Elective VI', 'Open elective.', 3, 40, 10, 'Semester 8', 8, 'ELECTIVE', TRUE, FALSE)
ON CONFLICT (code) DO UPDATE
SET title = EXCLUDED.title,
    description = EXCLUDED.description,
    credit_hours = EXCLUDED.credit_hours,
    capacity = EXCLUDED.capacity,
    enrolled_count = EXCLUDED.enrolled_count,
    term = EXCLUDED.term,
    semester_number = EXCLUDED.semester_number,
    category = EXCLUDED.category,
    is_elective = EXCLUDED.is_elective,
    is_lab = EXCLUDED.is_lab;

-- Semester progress baseline
WITH student_cte AS (
    SELECT id AS student_id FROM students WHERE roll_number IN ('23I-0013', '23I-0661', '23I-0810')
)
INSERT INTO semester_progress (student_id, semester_number, status, earned_credits, semester_gpa, finalized_at)
SELECT student_id, semester_number, status, earned_credits, semester_gpa, finalized_at
FROM (
         VALUES
             (1, 'FINALIZED', 18, 3.60, TIMESTAMP '2022-06-15 00:00:00'),
             (2, 'FINALIZED', 18, 3.55, TIMESTAMP '2022-12-20 00:00:00'),
             (3, 'FINALIZED', 18, 3.45, TIMESTAMP '2023-06-18 00:00:00'),
             (4, 'FINALIZED', 18, 3.40, TIMESTAMP '2023-12-18 00:00:00'),
             (5, 'ACTIVE', 0, NULL, NULL)
     ) AS progress(semester_number, status, earned_credits, semester_gpa, finalized_at)
         CROSS JOIN student_cte
ON CONFLICT (student_id, semester_number) DO UPDATE
SET status = EXCLUDED.status,
    earned_credits = EXCLUDED.earned_credits,
    semester_gpa = EXCLUDED.semester_gpa,
    finalized_at = EXCLUDED.finalized_at;

-- Course enrollments with grades
WITH student_cte AS (
    SELECT id AS student_id FROM students WHERE roll_number IN ('23I-0013', '23I-0661', '23I-0810')
)
INSERT INTO enrollments (student_id, course_id, status, grade, repeat_required, section)
SELECT student_id,
       (SELECT id FROM courses WHERE code = course_code),
       status,
       grade,
       repeat_required,
       section
FROM (
         VALUES
             ('NS1001', 'COMPLETED', 'A', FALSE, 'A'),
             ('MT1003', 'COMPLETED', 'A', FALSE, 'A'),
             ('SS1012', 'COMPLETED', 'B+', FALSE, 'A'),
             ('CL1000', 'COMPLETED', 'A-', FALSE, 'A'),
             ('CS1002', 'COMPLETED', 'A', FALSE, 'A'),
             ('CS1004', 'COMPLETED', 'A-', FALSE, 'A'),
             ('CS2001', 'COMPLETED', 'B+', FALSE, 'A'),
             ('CS2005', 'COMPLETED', 'B', FALSE, 'A'),
             ('CS2006', 'COMPLETED', 'B', FALSE, 'A'),
             ('MT2005', 'COMPLETED', 'A-', FALSE, 'A'),
             ('CS3001', 'ENROLLED', NULL, FALSE, 'A'),
             ('CS2009', 'ENROLLED', NULL, FALSE, 'A'),
             ('CS3005', 'ENROLLED', NULL, FALSE, 'A'),
             ('CS3009', 'ENROLLED', NULL, FALSE, 'A'),
             ('AI2002', 'ENROLLED', NULL, FALSE, 'A')
     ) AS enrollment_data(course_code, status, grade, repeat_required, section)
         CROSS JOIN student_cte
ON CONFLICT (student_id, course_id) DO UPDATE
SET status = EXCLUDED.status,
    grade = EXCLUDED.grade,
    repeat_required = EXCLUDED.repeat_required,
    section = EXCLUDED.section;

INSERT INTO course_instructor_assignments (course_id, faculty_id, section, term)
VALUES
    ((SELECT id FROM courses WHERE code = 'CS3001'), (SELECT id FROM faculty WHERE email = 'tzk@nu.edu.pk'), 'A', 'Semester 5'),
    ((SELECT id FROM courses WHERE code = 'CS2009'), (SELECT id FROM faculty WHERE email = 'tzk@nu.edu.pk'), 'A', 'Semester 5'),
    ((SELECT id FROM courses WHERE code = 'CS3005'), (SELECT id FROM faculty WHERE email = 'tzk@nu.edu.pk'), 'A', 'Semester 5'),
    ((SELECT id FROM courses WHERE code = 'CS3009'), (SELECT id FROM faculty WHERE email = 'tzk@nu.edu.pk'), 'A', 'Semester 5'),
    ((SELECT id FROM courses WHERE code = 'AI2002'), (SELECT id FROM faculty WHERE email = 'tzk@nu.edu.pk'), 'A', 'Semester 5')
ON CONFLICT (course_id, section) DO UPDATE
SET faculty_id = EXCLUDED.faculty_id,
    term = EXCLUDED.term;

INSERT INTO feedback_questions (prompt, sort_order, is_active)
VALUES
    ('The instructor explained concepts clearly.', 1, TRUE),
    ('Course material and examples were relevant to learning outcomes.', 2, TRUE),
    ('The instructor encouraged participation and questions.', 3, TRUE),
    ('Assessment and grading criteria were communicated fairly.', 4, TRUE),
    ('Overall, I am satisfied with this instructor''s teaching.', 5, TRUE)
ON CONFLICT (prompt) DO NOTHING;

-- Attendance records for the active semester
WITH student_cte AS (
    SELECT id AS student_id FROM students WHERE roll_number IN ('23I-0013', '23I-0661', '23I-0810')
)
INSERT INTO attendance_records (student_id, course_id, attendance_date, status)
SELECT student_id,
       (SELECT id FROM courses WHERE code = course_code),
       attendance_date,
       status
FROM (
         VALUES
             ('CS3001', DATE '2024-02-05', 'PRESENT'),
             ('CS3001', DATE '2024-02-08', 'PRESENT'),
             ('CS3001', DATE '2024-02-12', 'ABSENT'),
             ('CS3001', DATE '2024-02-15', 'PRESENT'),
             ('CS3001', DATE '2024-02-19', 'PRESENT'),

             ('CS2009', DATE '2024-02-06', 'PRESENT'),
             ('CS2009', DATE '2024-02-09', 'PRESENT'),
             ('CS2009', DATE '2024-02-13', 'PRESENT'),
             ('CS2009', DATE '2024-02-16', 'ABSENT'),
             ('CS2009', DATE '2024-02-20', 'PRESENT'),

             ('CS3005', DATE '2024-02-07', 'PRESENT'),
             ('CS3005', DATE '2024-02-10', 'ABSENT'),
             ('CS3005', DATE '2024-02-14', 'PRESENT'),
             ('CS3005', DATE '2024-02-17', 'PRESENT'),
             ('CS3005', DATE '2024-02-21', 'PRESENT'),

             ('CS3009', DATE '2024-02-05', 'PRESENT'),
             ('CS3009', DATE '2024-02-08', 'PRESENT'),
             ('CS3009', DATE '2024-02-12', 'PRESENT'),
             ('CS3009', DATE '2024-02-15', 'PRESENT'),
             ('CS3009', DATE '2024-02-19', 'ABSENT'),

             ('AI2002', DATE '2024-02-06', 'PRESENT'),
             ('AI2002', DATE '2024-02-09', 'PRESENT'),
             ('AI2002', DATE '2024-02-13', 'PRESENT'),
             ('AI2002', DATE '2024-02-16', 'PRESENT'),
             ('AI2002', DATE '2024-02-20', 'PRESENT')
     ) AS attendance(course_code, attendance_date, status)
         CROSS JOIN student_cte
ON CONFLICT (student_id, course_id, attendance_date) DO UPDATE
SET status = EXCLUDED.status;

-- Assessment records mirroring LMS entries
WITH student_cte AS (
    SELECT id AS student_id FROM students WHERE roll_number IN ('23I-0013', '23I-0661', '23I-0810')
)
INSERT INTO assessment_records (student_id, course_id, category, title, obtained_marks, total_marks, absolute_weight)
SELECT student_id,
       (SELECT id FROM courses WHERE code = course_code),
       category,
       title,
       obtained_marks,
       total_marks,
       absolute_weight
FROM (
         VALUES
             ('CS3001', 'QUIZ', 'Quiz 1', 8.0, 10.0, 2.0),
             ('CS3001', 'QUIZ', 'Quiz 2', 7.5, 10.0, 2.0),
             ('CS3001', 'ASSIGNMENT', 'Assignment 1', 18.0, 20.0, 5.0),
             ('CS3001', 'PROJECT', 'Lab Project', 42.0, 50.0, 10.0),
             ('CS3001', 'SESSIONAL1', 'Sessional I', 25.0, 30.0, 15.0),
             ('CS3001', 'SESSIONAL2', 'Sessional II', 26.0, 30.0, 15.0),

             ('CS2009', 'QUIZ', 'Quiz 1', 9.0, 10.0, 2.0),
             ('CS2009', 'QUIZ', 'Quiz 2', 8.0, 10.0, 2.0),
             ('CS2009', 'ASSIGNMENT', 'Assignment 1', 19.0, 20.0, 5.0),
             ('CS2009', 'PROJECT', 'Algorithm Lab', 45.0, 50.0, 10.0),
             ('CS2009', 'SESSIONAL1', 'Sessional I', 24.0, 30.0, 15.0),
             ('CS2009', 'SESSIONAL2', 'Sessional II', 27.0, 30.0, 15.0),

             ('CS3005', 'QUIZ', 'Quiz 1', 7.0, 10.0, 2.0),
             ('CS3005', 'QUIZ', 'Quiz 2', 8.5, 10.0, 2.0),
             ('CS3005', 'ASSIGNMENT', 'Problem Set', 17.0, 20.0, 5.0),
             ('CS3005', 'PROJECT', 'Grammar Project', 40.0, 50.0, 10.0),
             ('CS3005', 'SESSIONAL1', 'Sessional I', 23.0, 30.0, 15.0),
             ('CS3005', 'SESSIONAL2', 'Sessional II', 22.0, 30.0, 15.0),

             ('CS3009', 'QUIZ', 'Quiz 1', 9.5, 10.0, 2.0),
             ('CS3009', 'QUIZ', 'Quiz 2', 9.0, 10.0, 2.0),
             ('CS3009', 'ASSIGNMENT', 'Design Doc', 19.0, 20.0, 5.0),
             ('CS3009', 'PROJECT', 'Sprint Demo', 48.0, 50.0, 10.0),
             ('CS3009', 'SESSIONAL1', 'Sessional I', 27.0, 30.0, 15.0),
             ('CS3009', 'SESSIONAL2', 'Sessional II', 28.0, 30.0, 15.0),

             ('AI2002', 'QUIZ', 'Quiz 1', 8.5, 10.0, 2.0),
             ('AI2002', 'ASSIGNMENT', 'Search Assignment', 19.0, 20.0, 5.0),
             ('AI2002', 'PROJECT', 'Agent Project', 44.0, 50.0, 10.0),
             ('AI2002', 'SESSIONAL1', 'Sessional I', 26.0, 30.0, 15.0)
     ) AS assessments(course_code, category, title, obtained_marks, total_marks, absolute_weight)
         CROSS JOIN student_cte;

-- Fee challan and line items managed via DB
WITH student_cte AS (
    SELECT id AS student_id, roll_number FROM students WHERE roll_number IN ('23I-0013', '23I-0661', '23I-0810')
)
INSERT INTO fee_challans (student_id, challan_number, issue_date, due_date, total_amount, total_credit_hours)
SELECT student_id,
       'FAST-' || roll_number || '-20240315',
       DATE '2024-03-15',
       DATE '2024-03-25',
       123500.00,
       15
FROM student_cte
ON CONFLICT (challan_number) DO UPDATE
SET total_amount = EXCLUDED.total_amount,
    total_credit_hours = EXCLUDED.total_credit_hours,
    issue_date = EXCLUDED.issue_date,
    due_date = EXCLUDED.due_date;

WITH challan_cte AS (
    SELECT id FROM fee_challans WHERE challan_number LIKE 'FAST-23I-%-20240315'
)
INSERT INTO fee_line_items (challan_id, code, title, credit_hours, amount)
SELECT id,
       code,
       title,
       credit_hours,
       amount
FROM challan_cte
CROSS JOIN (
    VALUES
        ('CS3001', 'Computer Networks', 3, 18600.00),
        ('CS2009', 'Design and Analysis of Algorithms', 3, 18600.00),
        ('CS3005', 'Theory of Automata', 3, 18600.00),
        ('CS3009', 'Software Engineering', 3, 18600.00),
        ('AI2002', 'Artificial Intelligence', 3, 18600.00),
        ('LAB', 'Network Lab Surcharge', 0, 1800.00),
        ('TECH', 'Technology services bundle', 0, 1200.00),
        ('ACT', 'Student activity fund', 0, 1500.00)
) AS fee_items(code, title, credit_hours, amount);
