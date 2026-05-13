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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private AcademicRulesService academicRulesService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private Student student;
    private Course course;

    @BeforeEach
    void setUp() {
        student = new Student();
        ReflectionTestUtils.setField(student, "id", 1L);
        student.setCurrentSemester(3);

        course = new Course();
        ReflectionTestUtils.setField(course, "id", 10L);
        course.setCode("CS301");
        course.setTitle("Algorithms");
        course.setCreditHours(3);
        course.setSemesterNumber(3);
        course.setCapacity(2);
        course.setEnrolledCount(0);
    }

    @Test
    void getEnrollmentsForStudentRejectsMissingStudent() {
        when(studentRepository.existsById(1L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> enrollmentService.getEnrollmentsForStudent(1L));
        assertEquals("Student not found", exception.getMessage());
    }

    @Test
    void getEnrollmentsForStudentRefreshesRepeatFlagsAndReturnsRows() {
        when(studentRepository.existsById(1L)).thenReturn(true);
        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of());

        assertTrue(enrollmentService.getEnrollmentsForStudent(1L).isEmpty());
        verify(academicRulesService).refreshRepeatFlags(1L);
    }

    @Test
    void registerCourseCreatesNewEnrollmentAndRejectsCapacityIssues() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentIdAndCourseId(1L, 10L)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Enrollment saved = enrollmentService.registerCourse(request());

        assertAll(
                () -> assertEquals(EnrollmentStatus.ENROLLED, saved.getStatus()),
                () -> assertEquals(1, course.getEnrolledCount())
        );

        course.setEnrolledCount(2);
        when(enrollmentRepository.findByStudentIdAndCourseId(1L, 10L)).thenReturn(Optional.empty());
        BadRequestException full = assertThrows(BadRequestException.class, () -> enrollmentService.registerCourse(request()));
        assertEquals("Course is full", full.getMessage());
    }

    @Test
    void registerCourseHandlesExistingEnrollmentsAcrossAllBranches() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        doNothing().when(academicRulesService).validateRegistration(any(), any());
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Enrollment enrolled = enrollment(100L, EnrollmentStatus.ENROLLED);
        when(enrollmentRepository.findByStudentIdAndCourseId(1L, 10L)).thenReturn(Optional.of(enrolled));
        BadRequestException alreadyEnrolled = assertThrows(BadRequestException.class,
                () -> enrollmentService.registerCourse(request()));
        assertEquals("Student already enrolled in course", alreadyEnrolled.getMessage());

        Enrollment dropped = enrollment(101L, EnrollmentStatus.DROPPED);
        when(enrollmentRepository.findByStudentIdAndCourseId(1L, 10L)).thenReturn(Optional.of(dropped));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Enrollment reopened = enrollmentService.registerCourse(request());
        assertAll(
                () -> assertEquals(EnrollmentStatus.ENROLLED, reopened.getStatus()),
                () -> assertEquals(1, course.getEnrolledCount())
        );

        Enrollment completed = enrollment(102L, EnrollmentStatus.COMPLETED);
        when(enrollmentRepository.findByStudentIdAndCourseId(1L, 10L)).thenReturn(Optional.of(completed));
        BadRequestException completedException = assertThrows(BadRequestException.class,
                () -> enrollmentService.registerCourse(request()));
        assertEquals("Course already completed", completedException.getMessage());
    }

    @Test
    void dropCourseHandlesLockedAndSuccessfulBranches() {
        Enrollment enrollment = enrollment(200L, EnrollmentStatus.ENROLLED);
        enrollment.setGrade(null);
        enrollment.setCourse(course);
        enrollment.setStudent(student);
        when(enrollmentRepository.findByStudentIdAndCourseId(1L, 10L)).thenReturn(Optional.of(enrollment));
        when(academicRulesService.canDrop(enrollment)).thenReturn(false);

        BadRequestException locked = assertThrows(BadRequestException.class,
                () -> enrollmentService.dropCourse(dropRequest()));
        assertEquals("Course is locked and cannot be dropped", locked.getMessage());

        when(academicRulesService.canDrop(enrollment)).thenReturn(true);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        enrollmentService.dropCourse(dropRequest());

        assertEquals(EnrollmentStatus.DROPPED, enrollment.getStatus());
    }

    @Test
    void dropCourseRejectsMissingEnrollment() {
        when(enrollmentRepository.findByStudentIdAndCourseId(1L, 10L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> enrollmentService.dropCourse(dropRequest()));
        assertEquals("Enrollment not found", exception.getMessage());
    }

    private RegisterCourseRequest request() {
        RegisterCourseRequest request = new RegisterCourseRequest();
        request.setStudentId(1L);
        request.setCourseId(10L);
        return request;
    }

    private DropCourseRequest dropRequest() {
        DropCourseRequest request = new DropCourseRequest();
        request.setStudentId(1L);
        request.setCourseId(10L);
        return request;
    }

    private Enrollment enrollment(Long id, EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        ReflectionTestUtils.setField(enrollment, "id", id);
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus(status);
        return enrollment;
    }
}