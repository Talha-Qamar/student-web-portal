package com.studentportal.service;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private AcademicRulesService academicRulesService;

    @InjectMocks
    private CourseService courseService;

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student();
        ReflectionTestUtils.setField(student, "id", 1L);
        student.setCurrentSemester(3);
    }

    @Test
    void getAllCoursesAndCatalogBySemesterGroupAndFilterNullValues() {
        // Covers the grouping path and the branch that skips courses with null semester numbers.
        Course courseOne = course(1L, "CS101", 1);
        Course courseTwo = course(2L, "CS201", 2);
        Course courseWithoutSemester = course(3L, "CS999", null);
        when(courseRepository.findAll()).thenReturn(List.of(courseOne, courseTwo, courseWithoutSemester));

        assertAll(
                () -> assertEquals(3, courseService.getAllCourses().size()),
                () -> assertEquals(2, courseService.getCatalogBySemester().size()),
                () -> assertTrue(courseService.getCatalogBySemester().containsKey(1)),
                () -> assertTrue(courseService.getCatalogBySemester().containsKey(2))
        );
    }

    @Test
    void getAvailableCoursesForStudentCombinesCurrentSemesterAndRepeatShortlist() {
        // Covers the catalog shortlist loop, backlog repeat inclusion, and the branch that filters already-registered courses.
        Course currentOne = course(10L, "CS301", 3);
        Course currentTwo = course(11L, "CS302", 3);
        Course backlog = course(12L, "CS201", 2);
        Course alreadyRegistered = course(13L, "CS303", 3);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of(enrollment(alreadyRegistered, EnrollmentStatus.ENROLLED)));
        when(courseRepository.findBySemesterNumberOrderByCode(3)).thenReturn(List.of(currentOne, currentTwo));
        when(courseRepository.findAll()).thenReturn(List.of(currentOne, currentTwo, backlog, alreadyRegistered));
        when(academicRulesService.hasOutstandingRepeat(student, backlog)).thenReturn(true);

        List<Course> available = courseService.getAvailableCoursesForStudent(1L);

        assertAll(
                () -> assertEquals(3, available.size()),
                () -> assertTrue(available.contains(currentOne)),
                () -> assertTrue(available.contains(currentTwo)),
                () -> assertTrue(available.contains(backlog))
        );
    }

    @Test
    void getAvailableCoursesRejectsMissingStudentAndGetsCourseById() {
        // Covers the not-found branch and the direct course lookup success path.
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());
        ResourceNotFoundException notFound = assertThrows(ResourceNotFoundException.class,
                () -> courseService.getAvailableCoursesForStudent(1L));
        assertEquals("Student not found", notFound.getMessage());

        Course course = course(9L, "CS999", 4);
        when(courseRepository.findById(9L)).thenReturn(Optional.of(course));
        assertEquals(course, courseService.getCourse(9L));
    }

    private Course course(Long id, String code, Integer semester) {
        Course course = new Course();
        ReflectionTestUtils.setField(course, "id", id);
        course.setCode(code);
        course.setTitle(code + " title");
        course.setSemesterNumber(semester);
        return course;
    }

    private Enrollment enrollment(Course course, EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus(status);
        return enrollment;
    }
}