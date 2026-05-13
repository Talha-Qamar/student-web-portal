package com.studentportal.service;

import com.studentportal.dto.DropCourseRequest;
import com.studentportal.dto.RegisterCourseRequest;
import com.studentportal.exception.BadRequestException;
import com.studentportal.exception.ResourceNotFoundException;
import com.studentportal.model.Course;
import com.studentportal.model.Enrollment;
import com.studentportal.model.EnrollmentStatus;
import com.studentportal.model.Student;
import com.studentportal.repository.CourseRepository;
import com.studentportal.repository.EnrollmentRepository;
import com.studentportal.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final AcademicRulesService academicRulesService;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             StudentRepository studentRepository,
                             CourseRepository courseRepository,
                             AcademicRulesService academicRulesService) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.academicRulesService = academicRulesService;
    }

    public List<Enrollment> getEnrollmentsForStudent(Long studentId) {
        ensureStudentExists(studentId);
        academicRulesService.refreshRepeatFlags(studentId);
        return enrollmentRepository.findByStudentId(studentId);
    }

    public Enrollment registerCourse(RegisterCourseRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        academicRulesService.validateRegistration(student, course);

        Enrollment existingEnrollment = enrollmentRepository.findByStudentIdAndCourseId(student.getId(), course.getId())
                .orElse(null);

        if (existingEnrollment != null) {
            if (EnrollmentStatus.ENROLLED.equals(existingEnrollment.getStatus())) {
                throw new BadRequestException("Student already enrolled in course");
            }
            if (EnrollmentStatus.DROPPED.equals(existingEnrollment.getStatus())) {
                ensureCapacityAvailable(course);
                existingEnrollment.setStatus(EnrollmentStatus.ENROLLED);
                existingEnrollment.setGrade(null);
                existingEnrollment.setRepeatRequired(false);
                incrementSeatCount(course);
                Enrollment saved = enrollmentRepository.save(existingEnrollment);
                academicRulesService.refreshRepeatFlags(student.getId());
                return saved;
            }
            throw new BadRequestException("Course already completed");
        }

        ensureCapacityAvailable(course);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        enrollment.setSection("A");
        enrollment.setRepeatRequired(false);

        incrementSeatCount(course);

        Enrollment saved = enrollmentRepository.save(enrollment);
        academicRulesService.refreshRepeatFlags(student.getId());
        return saved;
    }

    private void ensureCapacityAvailable(Course course) {
        if (course.getCapacity() != null && safeEnrolledCount(course) >= course.getCapacity()) {
            throw new BadRequestException("Course is full");
        }
    }

    private void incrementSeatCount(Course course) {
        course.setEnrolledCount(safeEnrolledCount(course) + 1);
        courseRepository.save(course);
    }

    private int safeEnrolledCount(Course course) {
        return course.getEnrolledCount() == null ? 0 : course.getEnrolledCount();
    }

    public void dropCourse(DropCourseRequest request) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(
                        request.getStudentId(), request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (!academicRulesService.canDrop(enrollment)) {
            throw new BadRequestException("Course is locked and cannot be dropped");
        }

        Course course = enrollment.getCourse();
        course.setEnrolledCount(Math.max(0, course.getEnrolledCount() - 1));
        courseRepository.save(course);

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollmentRepository.save(enrollment);
    }

    private void ensureStudentExists(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found");
        }
    }
}
