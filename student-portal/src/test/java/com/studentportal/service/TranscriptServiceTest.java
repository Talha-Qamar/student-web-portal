package com.studentportal.service;

import com.studentportal.dto.SemesterTranscriptBlock;
import com.studentportal.dto.TranscriptResponse;
import com.studentportal.exception.ResourceNotFoundException;
import com.studentportal.model.Course;
import com.studentportal.model.Enrollment;
import com.studentportal.model.EnrollmentStatus;
import com.studentportal.model.SemesterProgress;
import com.studentportal.model.SemesterStatus;
import com.studentportal.model.Student;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscriptServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private AcademicRulesService academicRulesService;

    @InjectMocks
    private TranscriptService transcriptService;

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student();
        ReflectionTestUtils.setField(student, "id", 1L);
        student.setFullName("Ali Khan");
        student.setMajor("Computer Science");
        student.setEnrollmentYear(2022);
        student.setGpa(3.6);
    }

    @Test
    void getTranscriptRejectsMissingStudent() {
        // Covers the student lookup failure branch.
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> transcriptService.getTranscript(1L));
        assertEquals("Student not found", exception.getMessage());
    }

    @Test
    void getTranscriptSkipsDroppedEnrollmentsAndUsesSemesterFallbacks() {
        // Covers the outer 8-semester loop, the dropped-enrollment skip branch, and the fallback path for semester GPA/credits.
        Course completedCourse = course(11L, "CS301", 3, 3);
        Course droppedCourse = course(12L, "CS302", 3, 3);

        Enrollment completed = enrollment(101L, completedCourse, EnrollmentStatus.COMPLETED, "A", false);
        Enrollment dropped = enrollment(102L, droppedCourse, EnrollmentStatus.DROPPED, "F", true);
        SemesterProgress progress = progress(3, SemesterStatus.FINALIZED, 3.75, 3);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of(completed, dropped));
        when(academicRulesService.progressBySemester(1L)).thenReturn(Map.of(3, progress));
        when(academicRulesService.ensureSemesterProgress(any())).thenReturn(List.of(progress));

        TranscriptResponse response = transcriptService.getTranscript(1L);

        assertAll(
                () -> assertEquals("Ali Khan", response.getStudentName()),
                () -> assertEquals(1, response.getSemesters().stream().filter(SemesterTranscriptBlock::isFinalized).count()),
                () -> assertEquals(1, response.getCourses().size()),
                () -> assertEquals("CS301", response.getCourses().get(0).getCode()),
                () -> assertEquals(3.75, response.getSemesters().stream().filter(block -> block.getSemesterNumber() == 3).findFirst().orElseThrow().getSemesterGpa())
        );
    }

    @Test
    void exportTranscriptPdfReturnsBytes() {
        // Covers the PDF-generation success path after transcript building.
        Course course = course(21L, "CS301", 3, 3);
        Enrollment enrollment = enrollment(201L, course, EnrollmentStatus.COMPLETED, "A", false);
        SemesterProgress progress = progress(3, SemesterStatus.FINALIZED, 4.0, 3);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of(enrollment));
        when(academicRulesService.progressBySemester(1L)).thenReturn(Map.of(3, progress));
        when(academicRulesService.ensureSemesterProgress(any())).thenReturn(List.of(progress));

        byte[] pdf = transcriptService.exportTranscriptPdf(1L);

        assertAll(
                () -> assertNotNull(pdf),
                () -> assertTrue(pdf.length > 0)
        );
    }

    @Test
    void getTranscriptHandlesNullGradeAndMissingCreditsInFallbackPath() {
        // Covers the null-grade / null-credit fallback inside the transcript calculation loop.
        Course course = course(31L, "CS401", 4, null);
        Enrollment enrollment = enrollment(301L, course, EnrollmentStatus.ENROLLED, null, false);
        SemesterProgress progress = progress(4, SemesterStatus.ACTIVE, null, null);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of(enrollment));
        when(academicRulesService.progressBySemester(1L)).thenReturn(Map.of(4, progress));
        when(academicRulesService.ensureSemesterProgress(any())).thenReturn(List.of(progress));

        TranscriptResponse response = transcriptService.getTranscript(1L);

        assertEquals(0.0, response.getSemesters().stream().filter(block -> block.getSemesterNumber() == 4).findFirst().orElseThrow().getSemesterGpa());
    }

    private Course course(Long id, String code, int semester, Integer credits) {
        Course course = new Course();
        ReflectionTestUtils.setField(course, "id", id);
        course.setCode(code);
        course.setTitle(code + " title");
        course.setSemesterNumber(semester);
        course.setCreditHours(credits);
        course.setTerm("Fall");
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

    private SemesterProgress progress(int semester, SemesterStatus status, Double gpa, Integer credits) {
        SemesterProgress progress = new SemesterProgress();
        progress.setStudent(student);
        progress.setSemesterNumber(semester);
        progress.setStatus(status);
        progress.setSemesterGpa(gpa);
        progress.setEarnedCredits(credits);
        return progress;
    }
}