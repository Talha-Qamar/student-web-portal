package com.studentportal.service;

import com.studentportal.exception.BadRequestException;
import com.studentportal.exception.ResourceNotFoundException;
import com.studentportal.model.Course;
import com.studentportal.model.CourseInstructorAssignment;
import com.studentportal.model.Enrollment;
import com.studentportal.model.EnrollmentStatus;
import com.studentportal.model.Faculty;
import com.studentportal.model.FacultyFeedback;
import com.studentportal.model.FeedbackQuestion;
import com.studentportal.model.Student;
import com.studentportal.repository.CourseInstructorAssignmentRepository;
import com.studentportal.repository.EnrollmentRepository;
import com.studentportal.repository.FacultyFeedbackRepository;
import com.studentportal.repository.FacultyFeedbackResponseRepository;
import com.studentportal.repository.FeedbackQuestionRepository;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class StudentFeedbackServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseInstructorAssignmentRepository assignmentRepository;

    @Mock
    private FeedbackQuestionRepository questionRepository;

    @Mock
    private FacultyFeedbackRepository feedbackRepository;

    @Mock
    private FacultyFeedbackResponseRepository responseRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentFeedbackService studentFeedbackService;

    private Student student;
    private Course course;
    private CourseInstructorAssignment assignment;
    private FeedbackQuestion question1;
    private FeedbackQuestion question2;

    @BeforeEach
    void setUp() {
        student = new Student();
        ReflectionTestUtils.setField(student, "id", 1L);

        course = new Course();
        ReflectionTestUtils.setField(course, "id", 11L);
        course.setCode("CS301");
        course.setTitle("Algorithms");

        Faculty faculty = new Faculty();
        ReflectionTestUtils.setField(faculty, "id", 2L);
        faculty.setFullName("Dr. Sara");

        assignment = new CourseInstructorAssignment();
        ReflectionTestUtils.setField(assignment, "id", 100L);
        assignment.setCourse(course);
        assignment.setFaculty(faculty);
        assignment.setSection("A");

        question1 = new FeedbackQuestion();
        ReflectionTestUtils.setField(question1, "id", 1L);
        question1.setPrompt("Q1");
        question1.setSortOrder(1);

        question2 = new FeedbackQuestion();
        ReflectionTestUtils.setField(question2, "id", 2L);
        question2.setPrompt("Q2");
        question2.setSortOrder(2);
    }

    @Test
    void getQuestionsReturnsActiveOrderedItemsAndEligibleAssignmentsFiltersByStatus() {
        // Covers eligible-status filtering, assignment lookup, submitted-state detection, and question ordering.
        when(questionRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(question1, question2));
        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of(enrollment(EnrollmentStatus.ENROLLED), enrollment(EnrollmentStatus.DROPPED)));
        when(assignmentRepository.findByCourseIdAndSectionIgnoreCase(11L, "A")).thenReturn(Optional.of(assignment));
        when(feedbackRepository.findByAssignmentIdAndStudentId(100L, 1L)).thenReturn(Optional.empty());

        assertAll(
                () -> assertEquals(2, studentFeedbackService.getQuestions().size()),
                () -> assertEquals(1, studentFeedbackService.getEligibleAssignments(1L).size()),
                () -> assertTrue(studentFeedbackService.getEligibleAssignments(1L).get(0).facultyName().contains("Sara"))
        );
    }

    @Test
    void submitFeedbackRejectsInvalidInputsAndBadSections() {
        // Covers the empty-ratings branch, missing-assignment branch, not-enrolled branch, and section-mismatch branch.
        BadRequestException empty = assertThrows(BadRequestException.class,
                () -> studentFeedbackService.submitFeedback(1L, 100L, null));
        assertEquals("Please answer all feedback questions", empty.getMessage());

        when(assignmentRepository.findById(100L)).thenReturn(Optional.empty());
        ResourceNotFoundException assignmentMissing = assertThrows(ResourceNotFoundException.class,
                () -> studentFeedbackService.submitFeedback(1L, 100L, Map.of(1L, 3)));
        assertEquals("Assigned course section not found", assignmentMissing.getMessage());

        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));
        when(enrollmentRepository.findByStudentIdAndCourseId(1L, 11L)).thenReturn(Optional.empty());
        BadRequestException notEnrolled = assertThrows(BadRequestException.class,
                () -> studentFeedbackService.submitFeedback(1L, 100L, Map.of(1L, 3)));
        assertEquals("You are not enrolled in this course", notEnrolled.getMessage());

        when(enrollmentRepository.findByStudentIdAndCourseId(1L, 11L)).thenReturn(Optional.of(enrollment(EnrollmentStatus.ENROLLED)));
        Enrollment enrolled = enrollment(EnrollmentStatus.ENROLLED);
        enrolled.setSection("B");
        when(enrollmentRepository.findByStudentIdAndCourseId(1L, 11L)).thenReturn(Optional.of(enrolled));
        BadRequestException wrongSection = assertThrows(BadRequestException.class,
                () -> studentFeedbackService.submitFeedback(1L, 100L, Map.of(1L, 3)));
        assertEquals("Feedback is only allowed for your enrolled section", wrongSection.getMessage());
    }

    @Test
    void submitFeedbackSavesResponsesAndRejectsInvalidRatings() {
        // Covers the happy path through feedback save plus the loop over questions and the invalid-rating branch.
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));
        Enrollment enrolled = enrollment(EnrollmentStatus.ENROLLED);
        enrolled.setSection("A");
        when(enrollmentRepository.findByStudentIdAndCourseId(1L, 11L)).thenReturn(Optional.of(enrolled));
        when(feedbackRepository.findByAssignmentIdAndStudentId(100L, 1L)).thenReturn(Optional.empty());
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(questionRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(question1, question2));
        when(feedbackRepository.save(any(FacultyFeedback.class))).thenAnswer(invocation -> {
            FacultyFeedback saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 999L);
            return saved;
        });
        doNothing().when(responseRepository).deleteByFeedbackId(999L);

        BadRequestException invalidRating = assertThrows(BadRequestException.class,
                () -> studentFeedbackService.submitFeedback(1L, 100L, Map.of(1L, 6, 2L, 4)));
        assertEquals("Each question must have a rating from 1 to 5", invalidRating.getMessage());

        studentFeedbackService.submitFeedback(1L, 100L, Map.of(1L, 5, 2L, 4));
        verify(responseRepository, times(2)).deleteByFeedbackId(999L);
    }

    @Test
    void submitFeedbackRejectsMissingStudent() {
        // Covers the missing-student exception after feedback and assignment validation pass.
        when(assignmentRepository.findById(100L)).thenReturn(Optional.of(assignment));
        when(enrollmentRepository.findByStudentIdAndCourseId(1L, 11L)).thenReturn(Optional.of(enrollment(EnrollmentStatus.ENROLLED)));
        when(feedbackRepository.findByAssignmentIdAndStudentId(100L, 1L)).thenReturn(Optional.empty());
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> studentFeedbackService.submitFeedback(1L, 100L, Map.of(1L, 3)));
        assertEquals("Student not found", exception.getMessage());
    }

    private Enrollment enrollment(EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus(status);
        enrollment.setSection("A");
        return enrollment;
    }
}