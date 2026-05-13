# Student Portal - NUCES

A comprehensive web-based student management system built for NUCES (National University of Computer and Emerging Sciences). This full-stack application enables efficient management of student enrollments, academic records, attendance tracking, and course feedback across multiple stakeholder roles.

**GitHub Repository**: [Talha-Qamar/student-web-portal](https://github.com/Talha-Qamar/student-web-portal)

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [System Architecture](#system-architecture)
- [Database Schema](#database-schema)
- [Backend Structure](#backend-structure)
- [Frontend Structure](#frontend-structure)
- [Installation & Setup](#installation--setup)
- [Running the Application](#running-the-application)
- [API Endpoints](#api-endpoints)
- [Usage Guide](#usage-guide)
- [Testing](#testing)
- [Team Members](#team-members)

---

## Overview

The Student Portal is a multi-role application designed to streamline academic operations at NUCES. It provides dedicated interfaces for:
- **Students**: Course enrollment, transcript viewing, fee management, and attendance tracking
- **Faculty**: Course management, student performance assessment, and attendance recording
- **Administrators**: System-wide user and course management, configuration, and reporting

The application features a responsive Vaadin-based frontend with a robust Spring Boot backend, utilizing PostgreSQL for data persistence.

---

## ✨ Features

### 🎓 Student Features
- **Authentication & Dashboard**
  - Secure login with email and password
  - Personalized student dashboard with quick statistics
  - Real-time access to important information

- **Course Enrollment**
  - Browse available courses with detailed information
  - Register for courses (within capacity limits)
  - Drop courses during enrollment period
  - View enrolled courses and sections
  - Prerequisite validation

- **Transcript Management**
  - View complete academic history
  - Display grades for all completed courses
  - Track cumulative and semester GPA
  - View credit hours earned

- **Semester Progress**
  - Monitor current semester GPA
  - Track earned credits per semester
  - View semester status (ongoing, completed, failed)
  - Academic performance analytics

- **Fee Management**
  - View fee challans (bills)
  - Track payment status
  - Download challan documents (PDF generation)
  - Payment history and records

- **Attendance Tracking**
  - Monitor attendance percentage per course
  - Real-time attendance updates
  - Alerts for low attendance
  - Historical attendance records

- **Faculty Feedback**
  - Submit feedback on courses
  - Rate course quality and instructor performance
  - View submitted feedback history
  - Anonymous feedback option

- **Assessment Records**
  - View assignment scores
  - Track quiz and exam marks
  - Monitor assessment progress
  - Performance comparison with class

### 👨‍🏫 Faculty Features
- **Course Management**
  - View assigned courses and sections
  - Manage course details
  - View enrolled students list
  - Track course capacity and enrollments

- **Student Performance**
  - View student list for each course
  - Record and update grades
  - Monitor enrollment changes
  - Generate performance reports

- **Attendance Management**
  - Mark daily attendance
  - Update attendance records
  - Generate attendance reports
  - Track attendance trends

- **Assessment Scoring**
  - Input assignment scores
  - Record exam marks
  - Update quiz results
  - Manage assessment categories

- **Feedback Collection**
  - View student feedback on courses
  - Collect structured feedback
  - Generate feedback reports
  - Analyze feedback for improvements

- **Course Feedback**
  - Provide feedback on course structure
  - Rate student performance
  - Suggest curriculum improvements
  - Document feedback history

### 🔧 Admin Features
- **User Management**
  - Create, update, delete student accounts
  - Manage faculty accounts
  - Manage admin accounts
  - Bulk user operations

- **Course Administration**
  - Create new courses
  - Update course information
  - Delete courses
  - Manage course capacity and prerequisites
  - Assign course categories (Core, Elective, Lab)

- **Instructor Assignment**
  - Assign faculty to courses
  - Update course instructors
  - Manage faculty workload
  - Track instructor assignments

- **Fee Challan Management**
  - Generate fee challans for students
  - Set fee amounts per semester
  - Track payment status
  - Manage fee records

- **System Configuration**
  - Configure assessment categories and weights
  - Set GPA calculation parameters
  - Manage academic settings
  - Define attendance thresholds

- **Semester Management**
  - Create and manage semesters
  - Set semester dates
  - Manage semester status
  - Control enrollment periods

---

## 🛠 Technology Stack

### Backend
- **Framework**: Spring Boot 3.2.3
- **Language**: Java 17
- **ORM**: Spring Data JPA with Hibernate
- **Build Tool**: Maven 3.6+
- **Testing**: JUnit 5 (with test cases for controllers and services)

### Frontend
- **UI Framework**: Vaadin 24.4.6
- **Styling**: Vaadin Lumo Theme (Customizable)
- **Components**: Pre-built Vaadin components (Grid, Form, Button, ComboBox, DatePicker, etc.)
- **Generated Code**: TypeScript/JavaScript for client-side interactions

### Database
- **Primary**: PostgreSQL
- **Connection**: JDBC with SSL support (Cloud-hosted on Neon)
- **Schema Management**: Hibernate DDL Auto with SQL initialization scripts

### Additional Libraries
- **PDF Generation**: OpenPDF 1.3.32 for fee challan PDF export
- **Validation**: Spring Boot Validation with custom validators
- **Security**: Spring Security (configurable for authentication)

---

## 🏗 System Architecture

The application follows a layered architecture pattern:

```
┌──────────────────────────────────┐
│   Browser / Vaadin Client        │
└───────────────┬──────────────────┘
                │
                ▼
┌──────────────────────────────────┐
│     Presentation Layer (UI)      │
│  - LoginView, DashboardView      │
│  - EnrollmentView, TranscriptView│
└───────────────┬──────────────────┘
                │
                ▼
┌──────────────────────────────────┐
│    Controller Layer (REST/API)   │
│  - EnrollmentController          │
│  - TranscriptController          │
│  - AdminController, etc.         │
└───────────────┬──────────────────┘
                │
                ▼
┌──────────────────────────────────┐
│   Service Layer (Business Logic) │
│  - EnrollmentService             │
│  - TranscriptService             │
│  - StudentService, etc.          │
└───────────────┬──────────────────┘
                │
                ▼
┌──────────────────────────────────┐
│  Repository Layer (Data Access)  │
│  - StudentRepository             │
│  - CourseRepository              │
│  - EnrollmentRepository, etc.    │
└───────────────┬──────────────────┘
                │
                ▼
┌──────────────────────────────────┐
│      PostgreSQL Database         │
└──────────────────────────────────┘
```

---

## 📊 Database Schema

The application uses PostgreSQL with the following main tables:

### Core Tables

**students** - Student account information
```
- id: BIGINT (Primary Key)
- full_name, roll_number, email, password
- major, enrollment_year, gpa, current_semester
- created_at: TIMESTAMP
```

**faculty** - Faculty information
```
- id: BIGINT (Primary Key)
- full_name, email, password
- department, designation
- created_at: TIMESTAMP
```

**admins** - Admin accounts
```
- id: BIGINT (Primary Key)
- full_name, email, password
- created_at: TIMESTAMP
```

**courses** - Course information
```
- id: BIGINT (Primary Key)
- code, title, description
- credit_hours, capacity, enrolled_count
- term, semester_number
- category (CORE/ELECTIVE/LAB)
- is_elective, is_lab, prerequisite_course_id
```

**enrollments** - Student course enrollments
```
- id: BIGINT (Primary Key)
- student_id, course_id (Foreign Keys)
- status (ENROLLED/DROPPED/COMPLETED/FAILED)
- grade, section, enrolled_at
- repeat_required, UNIQUE(student_id, course_id)
```

**semester_progress** - Student semester progress
```
- id: BIGINT (Primary Key)
- student_id, semester_number
- status, earned_credits, semester_gpa
- finalized_at, UNIQUE(student_id, semester_number)
```

**attendance_records** - Daily attendance
```
- id: BIGINT (Primary Key)
- student_id, course_id (Foreign Keys)
- attendance_date, status (PRESENT/ABSENT/LATE)
- recorded_at
```

**assessment_scores** - Assignment, quiz, exam scores
```
- id: BIGINT (Primary Key)
- student_id, course_id (Foreign Keys)
- category (ASSIGNMENT/QUIZ/EXAM)
- score, total_marks, recorded_date
```

**fee_challans** - Student fee records
```
- id: BIGINT (Primary Key)
- student_id, semester_number (Foreign Keys)
- amount, due_date
- payment_status (PENDING/PAID/OVERDUE)
- created_at
```

**feedback** - Course and faculty feedback
```
- id: BIGINT (Primary Key)
- student_id, course_id (Foreign Keys)
- rating (1-5), comments
- feedback_type (COURSE_FEEDBACK/FACULTY_FEEDBACK)
- submitted_at
```

---

## 🗂 Backend Structure

```
src/main/java/com/studentportal/
├── StudentPortalApplication.java        # Spring Boot entry point
├── config/                               # Configuration classes
│   ├── SecurityConfig.java
│   ├── VaadinConfig.java
│   └── DatabaseConfig.java
├── controller/                           # REST Controllers
│   ├── EnrollmentController.java
│   ├── TranscriptController.java
│   ├── AdminController.java
│   ├── FacultyController.java
│   ├── StudentController.java
│   ├── CourseController.java
│   ├── AttendanceController.java
│   └── FeedbackController.java
├── service/                              # Business Logic
│   ├── EnrollmentService.java
│   ├── TranscriptService.java
│   ├── StudentService.java
│   ├── CourseService.java
│   ├── FacultyService.java
│   ├── AdminService.java
│   ├── AttendanceService.java
│   ├── FeedbackService.java
│   ├── SessionService.java
│   ├── SemesterService.java
│   ├── FeeService.java
│   └── PDFGenerationService.java
├── repository/                           # Data Access (JPA)
│   ├── StudentRepository.java
│   ├── CourseRepository.java
│   ├── EnrollmentRepository.java
│   ├── FacultyRepository.java
│   ├── AdminRepository.java
│   ├── AttendanceRecordRepository.java
│   ├── SemesterProgressRepository.java
│   ├── AssessmentScoreRepository.java
│   ├── FeeChallonRepository.java
│   └── FeedbackRepository.java
├── model/                                # JPA Entities
│   ├── Student.java
│   ├── Faculty.java
│   ├── Admin.java
│   ├── Course.java
│   ├── Enrollment.java
│   ├── AttendanceRecord.java
│   ├── SemesterProgress.java
│   ├── AssessmentScore.java
│   ├── FeeChallan.java
│   └── Feedback.java
├── dto/                                  # Data Transfer Objects
│   ├── StudentDTO.java
│   ├── CourseDTO.java
│   ├── EnrollmentDTO.java
│   ├── TranscriptDTO.java
│   ├── AttendanceDTO.java
│   └── FeedbackDTO.java
├── exception/                            # Exception Handling
│   ├── StudentNotFoundException.java
│   ├── CourseNotFoundException.java
│   ├── EnrollmentException.java
│   ├── AuthenticationException.java
│   └── GlobalExceptionHandler.java
└── ui/                                   # Vaadin UI Views
    ├── LoginView.java
    ├── StudentDashboard.java
    ├── EnrollmentView.java
    ├── TranscriptView.java
    ├── AttendanceView.java
    ├── FacultyDashboard.java
    ├── AdminDashboard.java
    └── components/                       # Reusable Components
        ├── HeaderComponent.java
        ├── SidebarComponent.java
        ├── CourseCard.java
        └── GradeDisplay.java
```

---

## 🎨 Frontend Structure

```
src/main/frontend/
├── index.html                            # Main HTML
├── themes/
│   └── fast-portal/
│       ├── styles.css                    # Custom styles
│       └── theme.json                    # Theme config
└── generated/
    ├── theme.ts/.js                      # Generated theme
    └── flow/
        ├── generated-flow-imports.ts/.js # Auto-generated imports
        └── web-components/               # Component definitions
```

**Main Vaadin Views:**
- **LoginView** - Authentication interface
- **StudentDashboard** - Student main interface
- **EnrollmentView** - Course registration
- **TranscriptView** - Academic records
- **AttendanceView** - Attendance tracking
- **FacultyDashboard** - Faculty main interface
- **AdminDashboard** - Administration interface

---

## 🚀 Installation & Setup

### Prerequisites
- Java 17 or later
- Maven 3.6+
- PostgreSQL (cloud-hosted or local)
- Git

### Step 1: Clone the Repository

```bash
git clone https://github.com/Talha-Qamar/student-web-portal.git
cd student-web-portal/student-portal
```

### Step 2: Configure Database

Edit `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://your-host:5432/your-database
    username: your-username
    password: your-password
  jpa:
    hibernate:
      ddl-auto: update
```

### Step 3: Build the Project

```bash
mvn clean install
```

### Step 4: Run the Application

```bash
mvn spring-boot:run
```

### Step 5: Access the Application

Open browser to: `http://localhost:8080`

---

## 📖 Running the Application

### First-Time Setup

1. **Database**: Automatically initialized via Hibernate DDL Auto
2. **Sample Data**: Load from `database/sample-data.sql` (optional)
3. **Test Accounts**:
   - Student: student@example.com / password123
   - Faculty: faculty@example.com / password123
   - Admin: admin@example.com / password123

### Development Mode

```bash
mvn spring-boot:run
```

### Production Build

```bash
mvn clean package -DskipTests
java -jar target/student-portal-0.0.1-SNAPSHOT.jar
```

---

## 🔗 API Endpoints

### Student API
- `GET /api/students/{id}` - Get student profile
- `GET /api/students/{id}/courses` - Get enrolled courses
- `GET /api/students/{id}/transcript` - Get transcript
- `GET /api/students/{id}/attendance` - Get attendance

### Enrollment API
- `GET /api/enrollments/available-courses` - List available courses
- `POST /api/enrollments` - Enroll in course
- `DELETE /api/enrollments/{id}` - Drop course
- `GET /api/enrollments/my-courses` - My enrolled courses

### Course API
- `GET /api/courses` - Get all courses
- `POST /api/courses` - Create course (Admin)
- `GET /api/courses/{id}` - Get course details
- `PUT /api/courses/{id}` - Update course (Admin)
- `DELETE /api/courses/{id}` - Delete course (Admin)

### Faculty API
- `GET /api/faculty/{id}` - Get faculty profile
- `GET /api/faculty/{id}/courses` - Get assigned courses
- `POST /api/faculty/{id}/grades` - Record grades

### Admin API
- `POST /api/admin/users` - Create user
- `GET /api/admin/users` - List users
- `PUT /api/admin/users/{id}` - Update user
- `DELETE /api/admin/users/{id}` - Delete user

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout
- `POST /api/auth/register` - User registration

---

## 📝 Usage Guide

### For Students
1. **Login** with email and password
2. **Dashboard** - View stats and quick links
3. **Enroll** - Browse and register for courses
4. **Transcript** - View grades and GPA
5. **Attendance** - Monitor attendance
6. **Fees** - View and pay challans
7. **Feedback** - Submit course feedback

### For Faculty
1. **Login** with faculty credentials
2. **Dashboard** - View assigned courses
3. **Grades** - Enter student grades
4. **Attendance** - Mark daily attendance
5. **Assessment** - Record assignment/exam scores
6. **Feedback** - Review student feedback

### For Admins
1. **Login** with admin credentials
2. **Users** - Create and manage accounts
3. **Courses** - Create and manage courses
4. **Semesters** - Manage academic semesters
5. **Fees** - Generate and manage challans
6. **Settings** - Configure system parameters

---

## 🧪 Testing

### Run Tests

```bash
mvn test
```

### Test Classes
- `EnrollmentControllerTest.java` - Enrollment tests
- `TranscriptControllerTest.java` - Transcript tests
- `SessionServiceTest.java` - Session tests

---

## 👥 Team Members

**Group 3** - NUCES Semester 6 SE Project
- Roll Number: I230013
- Roll Number: I230661
- Roll Number: I230810

---

## 📄 License

Academic project for NUCES (National University of Computer and Emerging Sciences)

---

## 🔗 Repository

**GitHub**: [Talha-Qamar/student-web-portal](https://github.com/Talha-Qamar/student-web-portal)

---

**Last Updated**: May 2024