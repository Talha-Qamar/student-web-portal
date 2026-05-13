package com.studentportal.service;

import com.studentportal.exception.ResourceNotFoundException;
import com.studentportal.model.Course;
import com.studentportal.model.CourseInstructorAssignment;
import com.studentportal.model.Faculty;
import com.studentportal.model.Student;
import com.studentportal.repository.CourseInstructorAssignmentRepository;
import com.studentportal.repository.CourseRepository;
import com.studentportal.repository.FacultyRepository;
import com.studentportal.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AdminManagementService {

    private final CourseInstructorAssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final FacultyRepository facultyRepository;
    private final StudentRepository studentRepository;

    public AdminManagementService(CourseInstructorAssignmentRepository assignmentRepository,
                                  CourseRepository courseRepository,
                                  FacultyRepository facultyRepository,
                                  StudentRepository studentRepository) {
        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
        this.facultyRepository = facultyRepository;
        this.studentRepository = studentRepository;
    }

    public List<CourseInstructorAssignment> getAssignments() {
        return assignmentRepository.findAllByOrderByTermDescCourseCodeAscSectionAsc();
    }

    @Transactional
    public CourseInstructorAssignment assignInstructor(Long courseId, String section, Long facultyId, String term) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found"));

        String normalizedSection = normalizeSection(section);

        CourseInstructorAssignment assignment = assignmentRepository
                .findByCourseIdAndSectionIgnoreCase(courseId, normalizedSection)
                .orElseGet(CourseInstructorAssignment::new);

        assignment.setCourse(course);
        assignment.setFaculty(faculty);
        assignment.setSection(normalizedSection);
        assignment.setTerm((term == null || term.isBlank()) ? course.getTerm() : term.trim());
        return assignmentRepository.save(assignment);
    }

    public List<Course> getCourses() {
        return courseRepository.findAll().stream()
                .sorted((a, b) -> a.getCode().compareToIgnoreCase(b.getCode()))
                .toList();
    }

    public List<Faculty> getFacultyUsers() {
        return facultyRepository.findAll().stream()
                .sorted((a, b) -> a.getFullName().compareToIgnoreCase(b.getFullName()))
                .toList();
    }

    public List<Student> getStudents() {
        return studentRepository.findAll().stream()
                .sorted((a, b) -> a.getFullName().compareToIgnoreCase(b.getFullName()))
                .toList();
    }

    public Optional<Student> findStudentByRollNumber(String rollNumber) {
        if (rollNumber == null || rollNumber.isBlank()) {
            return Optional.empty();
        }
        return studentRepository.findByRollNumberIgnoreCase(rollNumber.trim());
    }

    @Transactional
    public Student updateStudent(Long studentId,
                                 String fullName,
                                 String rollNumber,
                                 String email,
                                 String major,
                                 Integer currentSemester) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        student.setFullName(fullName);
        student.setRollNumber(rollNumber);
        student.setEmail(email);
        student.setMajor(major);
        student.setCurrentSemester(currentSemester);
        return studentRepository.save(student);
    }

    @Transactional
    public Faculty updateFaculty(Long facultyId,
                                 String fullName,
                                 String email,
                                 String department,
                                 String designation) {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found"));
        faculty.setFullName(fullName);
        faculty.setEmail(email);
        faculty.setDepartment(department);
        faculty.setDesignation(designation);
        return facultyRepository.save(faculty);
    }

    private String normalizeSection(String section) {
        if (section == null || section.isBlank()) {
            return "A";
        }
        return section.trim().toUpperCase();
    }
}
