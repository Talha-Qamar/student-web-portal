package com.studentportal.service;

import com.studentportal.exception.BadRequestException;
import com.studentportal.model.Course;
import com.studentportal.model.Enrollment;
import com.studentportal.model.EnrollmentStatus;
import com.studentportal.model.SemesterProgress;
import com.studentportal.model.SemesterStatus;
import com.studentportal.model.Student;
import com.studentportal.repository.EnrollmentRepository;
import com.studentportal.repository.SemesterProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AcademicRulesServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private SemesterProgressRepository semesterProgressRepository;

    @InjectMocks
    private AcademicRulesService academicRulesService;

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student();
        ReflectionTestUtils.setField(student, "id", 1L);
        student.setCurrentSemester(3);
    }

    @Test
    void ensureSemesterProgressFinalizesEarlierSemestersAndActivatesCurrent() {
        // Covers the loop over semesters (1..8) and the branch that finalizes earlier semesters while activating the current one.
        SemesterProgress first = progress(1, SemesterStatus.ACTIVE, null);
        SemesterProgress third = progress(3, SemesterStatus.FINALIZED, null);
        when(semesterProgressRepository.findByStudentIdOrderBySemesterNumber(1L)).thenReturn(List.of(first, third));
        when(semesterProgressRepository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<SemesterProgress> input = invocation.getArgument(0);
            List<SemesterProgress> saved = new ArrayList<>();
            input.forEach(saved::add);
            return saved;
        });

        List<SemesterProgress> result = academicRulesService.ensureSemesterProgress(student);

        SemesterProgress semesterOne = result.stream().filter(p -> p.getSemesterNumber() == 1).findFirst().orElseThrow();
        SemesterProgress semesterThree = result.stream().filter(p -> p.getSemesterNumber() == 3).findFirst().orElseThrow();

        assertAll(
                () -> assertTrue(semesterOne.isFinalized()),
                () -> assertNotNull(semesterOne.getFinalizedAt()),
                () -> assertEquals(SemesterStatus.ACTIVE, semesterThree.getStatus())
        );
    }

    @Test
    void validateRegistrationRejectsNullCourseAndFuturePastLockedCases() {
        // Covers the null-course branch, future-semester rejection, past finalized-semester rejection, and locked-semester rejection.
        BadRequestException nullCourse = assertThrows(BadRequestException.class,
                () -> academicRulesService.validateRegistration(student, null));
        assertEquals("Course is required", nullCourse.getMessage());

        Course future = course(99L, "CS401", 4, 3);
        stubProgress();
        stubEmptyEnrollments();
        BadRequestException futureCourse = assertThrows(BadRequestException.class,
                () -> academicRulesService.validateRegistration(student, future));
        assertEquals("Clear previous semester before registering for CS401", futureCourse.getMessage());

        Course past = course(98L, "CS201", 2, 3);
        BadRequestException pastCourse = assertThrows(BadRequestException.class,
                () -> academicRulesService.validateRegistration(student, past));
        assertEquals("Semester 2 is finalized", pastCourse.getMessage());

        when(semesterProgressRepository.findByStudentIdAndSemesterNumber(1L, 3)).thenReturn(Optional.of(progress(3, SemesterStatus.FINALIZED, null)));
        Course locked = course(97L, "CS301", 3, 3);
        BadRequestException lockedCourse = assertThrows(BadRequestException.class,
                () -> academicRulesService.validateRegistration(student, locked));
        assertEquals("Semester is locked. No changes allowed.", lockedCourse.getMessage());
    }

    @Test
    void validateRegistrationRejectsCreditLimitAndPrerequisiteFailures() {
        // Covers the credit-limit branch and the prerequisite-not-cleared branch.
        Course overloaded = course(100L, "CS302", 3, 15);
        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of(enrollment(3L, overloaded, EnrollmentStatus.ENROLLED, null, false)));
        stubProgress();
        BadRequestException creditLimit = assertThrows(BadRequestException.class,
                () -> academicRulesService.validateRegistration(student, overloaded));
        assertEquals("Credit limit exceeded (max 18 CH)", creditLimit.getMessage());

        Course prerequisite = course(101L, "CS401", 3, 3);
        Course pre = course(102L, "CS301", 2, 3);
        prerequisite.setPrerequisite(pre);
        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of(enrollment(4L, pre, EnrollmentStatus.COMPLETED, "D", false)));
        BadRequestException prerequisiteFailure = assertThrows(BadRequestException.class,
                () -> academicRulesService.validateRegistration(student, prerequisite));
        assertEquals("Prerequisite CS301 not cleared", prerequisiteFailure.getMessage());
    }

    @Test
    void promotionRuleDetectsInsufficientCreditsAndCriticalFailures() {
        // Covers the promotion-rule path where previous-semester data exists and the rule blocks progression.
        Course previous = course(200L, "CS201", 2, 3);
        Enrollment completed = enrollment(10L, previous, EnrollmentStatus.COMPLETED, "C", false);
        Enrollment failed = enrollment(11L, previous, EnrollmentStatus.COMPLETED, "F", false);
        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of(completed, failed));
        stubProgress();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> academicRulesService.validateRegistration(student, course(201L, "CS301", 3, 3)));
        assertEquals("Clear previous semester before proceeding", exception.getMessage());
    }

    @Test
    void helperMethodsCoverDecisionBranches() {
        // Covers helper decisions: passing/critical grade checks, outstanding repeat detection, drop eligibility, and finalized-semester lookup.
        Course course = course(300L, "CS301", 3, 3);
        Course prereq = course(301L, "CS201", 2, 3);
        Enrollment passing = enrollment(20L, prereq, EnrollmentStatus.COMPLETED, "B", false);
        Enrollment failing = enrollment(21L, prereq, EnrollmentStatus.COMPLETED, "F", true);
        Enrollment dropped = enrollment(22L, course, EnrollmentStatus.DROPPED, "A", false);
        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of(passing, failing, dropped));
        when(semesterProgressRepository.findByStudentIdAndSemesterNumber(1L, 3)).thenReturn(Optional.of(progress(3, SemesterStatus.FINALIZED, null)));

        assertAll(
                () -> assertTrue(academicRulesService.isPassingGrade("a-")),
                () -> assertTrue(academicRulesService.isCriticalFailure("f")),
                () -> assertTrue(academicRulesService.hasOutstandingRepeat(student, prereq)),
                () -> assertFalse(academicRulesService.canDrop(dropped)),
                () -> assertTrue(academicRulesService.isSemesterFinalized(student, 3))
        );
    }

    @Test
    void refreshRepeatFlagsUpdatesDirtyEnrollmentsAndUpdateSummariesPersistsProgress() {
        // Covers the stream/loop path where repeat flags change and summaries are recalculated and saved.
        Course course = course(400L, "CS301", 2, 3);
        Enrollment enrollment = enrollment(30L, course, EnrollmentStatus.COMPLETED, "F", false);
        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of(enrollment));
        when(enrollmentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        academicRulesService.refreshRepeatFlags(1L);

        assertTrue(enrollment.isRepeatRequired());

        SemesterProgress progress = progress(2, SemesterStatus.ACTIVE, null);
        progress.setSemesterGpa(null);
        when(semesterProgressRepository.findByStudentIdOrderBySemesterNumber(1L)).thenReturn(List.of(progress));
        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of(enrollment));
        when(semesterProgressRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        academicRulesService.updateSummaries(1L);
        assertNotNull(progress.getSemesterGpa());
    }

    private void stubProgress() {
        when(semesterProgressRepository.findByStudentIdOrderBySemesterNumber(1L)).thenReturn(List.of(progress(1, SemesterStatus.FINALIZED, null), progress(2, SemesterStatus.FINALIZED, null)));
        when(semesterProgressRepository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<SemesterProgress> input = invocation.getArgument(0);
            List<SemesterProgress> saved = new ArrayList<>();
            input.forEach(saved::add);
            return saved;
        });
    }

    private void stubEmptyEnrollments() {
        when(enrollmentRepository.findByStudentId(1L)).thenReturn(new ArrayList<>());
    }

    private SemesterProgress progress(int semester, SemesterStatus status, java.time.LocalDateTime finalizedAt) {
        SemesterProgress progress = new SemesterProgress();
        progress.setStudent(student);
        progress.setSemesterNumber(semester);
        progress.setStatus(status);
        progress.setFinalizedAt(finalizedAt);
        return progress;
    }

    private Course course(Long id, String code, int semester, int credits) {
        Course course = new Course();
        ReflectionTestUtils.setField(course, "id", id);
        course.setCode(code);
        course.setTitle(code + " title");
        course.setSemesterNumber(semester);
        course.setCreditHours(credits);
        course.setEnrolledCount(0);
        return course;
    }

    private Enrollment enrollment(Long id, Course course, EnrollmentStatus status, String grade, boolean repeatRequired) {
        Enrollment enrollment = new Enrollment();
        ReflectionTestUtils.setField(enrollment, "id", id);
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus(status);
        enrollment.setGrade(grade);
        enrollment.setRepeatRequired(repeatRequired);
        return enrollment;
    }
}