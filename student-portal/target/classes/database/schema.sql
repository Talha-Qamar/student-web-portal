CREATE TABLE IF NOT EXISTS students (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    roll_number VARCHAR(20) NOT NULL,
    email VARCHAR(120) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    major VARCHAR(80),
    enrollment_year INT,
    gpa NUMERIC(3, 2),
    current_semester INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT NOW()
);

ALTER TABLE students
    ADD COLUMN IF NOT EXISTS roll_number VARCHAR(20);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'students_roll_number_key'
    ) THEN
        ALTER TABLE students
        ADD CONSTRAINT students_roll_number_key UNIQUE (roll_number);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS faculty (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(120) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    department VARCHAR(120),
    designation VARCHAR(120),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS admins (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(120) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS courses (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    credit_hours INT,
    capacity INT,
    enrolled_count INT DEFAULT 0,
    term VARCHAR(40),
    semester_number INT,
    category VARCHAR(20) DEFAULT 'CORE',
    is_elective BOOLEAN DEFAULT FALSE,
    is_lab BOOLEAN DEFAULT FALSE,
    prerequisite_course_id BIGINT REFERENCES courses(id)
);

CREATE TABLE IF NOT EXISTS enrollments (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT REFERENCES students(id) ON DELETE CASCADE,
    course_id BIGINT REFERENCES courses(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    grade VARCHAR(5),
    enrolled_at TIMESTAMP DEFAULT NOW(),
    repeat_required BOOLEAN DEFAULT FALSE,
    UNIQUE(student_id, course_id)
);

ALTER TABLE enrollments
    ADD COLUMN IF NOT EXISTS section VARCHAR(20) DEFAULT 'A';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'enrollments_student_course_unique'
    ) THEN
        ALTER TABLE enrollments
        ADD CONSTRAINT enrollments_student_course_unique UNIQUE (student_id, course_id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS semester_progress (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT REFERENCES students(id) ON DELETE CASCADE,
    semester_number INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    earned_credits INT DEFAULT 0,
    semester_gpa NUMERIC(4, 2),
    finalized_at TIMESTAMP,
    UNIQUE(student_id, semester_number)
);

CREATE TABLE IF NOT EXISTS attendance_records (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT REFERENCES students(id) ON DELETE CASCADE,
    course_id BIGINT REFERENCES courses(id) ON DELETE CASCADE,
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    UNIQUE(student_id, course_id, attendance_date)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'attendance_student_course_date_unique'
    ) THEN
        ALTER TABLE attendance_records
        ADD CONSTRAINT attendance_student_course_date_unique
            UNIQUE (student_id, course_id, attendance_date);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS assessment_records (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT REFERENCES students(id) ON DELETE CASCADE,
    course_id BIGINT REFERENCES courses(id) ON DELETE CASCADE,
    category VARCHAR(30) NOT NULL,
    title VARCHAR(150) NOT NULL,
    obtained_marks NUMERIC(6, 2),
    total_marks NUMERIC(6, 2),
    absolute_weight NUMERIC(5, 2)
);

CREATE TABLE IF NOT EXISTS course_instructor_assignments (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    faculty_id BIGINT NOT NULL REFERENCES faculty(id) ON DELETE CASCADE,
    section VARCHAR(20) NOT NULL,
    term VARCHAR(40),
    UNIQUE(course_id, section)
);

CREATE TABLE IF NOT EXISTS feedback_questions (
    id BIGSERIAL PRIMARY KEY,
    prompt VARCHAR(300) UNIQUE NOT NULL,
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS faculty_feedback (
    id BIGSERIAL PRIMARY KEY,
    assignment_id BIGINT NOT NULL REFERENCES course_instructor_assignments(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    submitted_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(assignment_id, student_id)
);

CREATE TABLE IF NOT EXISTS faculty_feedback_responses (
    id BIGSERIAL PRIMARY KEY,
    feedback_id BIGINT NOT NULL REFERENCES faculty_feedback(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES feedback_questions(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5)
);

CREATE TABLE IF NOT EXISTS fee_challans (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT REFERENCES students(id) ON DELETE CASCADE,
    challan_number VARCHAR(50) UNIQUE NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    total_credit_hours INT NOT NULL
);

CREATE TABLE IF NOT EXISTS fee_line_items (
    id BIGSERIAL PRIMARY KEY,
    challan_id BIGINT REFERENCES fee_challans(id) ON DELETE CASCADE,
    code VARCHAR(20),
    title VARCHAR(150) NOT NULL,
    credit_hours INT DEFAULT 0,
    amount NUMERIC(10, 2) NOT NULL
);
