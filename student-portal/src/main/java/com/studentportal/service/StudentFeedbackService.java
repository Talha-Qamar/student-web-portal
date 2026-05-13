package com.studentportal.service;

import com.studentportal.exception.BadRequestException;
import com.studentportal.exception.ResourceNotFoundException;
import com.studentportal.model.CourseInstructorAssignment;
import com.studentportal.model.Enrollment;
import com.studentportal.model.EnrollmentStatus;
import com.studentportal.model.FacultyFeedback;
import com.studentportal.model.FacultyFeedbackResponse;
import com.studentportal.model.FeedbackQuestion;
import com.studentportal.model.Student;
import com.studentportal.repository.CourseInstructorAssignmentRepository;
import com.studentportal.repository.EnrollmentRepository;
import com.studentportal.repository.FacultyFeedbackRepository;
import com.studentportal.repository.FacultyFeedbackResponseRepository;
import com.studentportal.repository.FeedbackQuestionRepository;
import com.studentportal.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StudentFeedbackService {

    private static final Set<EnrollmentStatus> FEEDBACK_ELIGIBLE_STATUSES =
            EnumSet.of(EnrollmentStatus.ENROLLED, EnrollmentStatus.COMPLETED);

    private final EnrollmentRepository enrollmentRepository;
    private final CourseInstructorAssignmentRepository assignmentRepository;
    private final FeedbackQuestionRepository questionRepository;
    private final FacultyFeedbackRepository feedbackRepository;
    private final FacultyFeedbackResponseRepository responseRepository;
    private final StudentRepository studentRepository;

    public StudentFeedbackService(EnrollmentRepository enrollmentRepository,
                                  CourseInstructorAssignmentRepository assignmentRepository,
                                  FeedbackQuestionRepository questionRepository,
                                  FacultyFeedbackRepository feedbackRepository,
                                  FacultyFeedbackResponseRepository responseRepository,
                                  StudentRepository studentRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.questionRepository = questionRepository;
        this.feedbackRepository = feedbackRepository;
        this.responseRepository = responseRepository;
        this.studentRepository = studentRepository;
    }

    public List<FeedbackQuestion> getQuestions() {
        return questionRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    public List<StudentAssignmentOption> getEligibleAssignments(Long studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId).stream()
                .filter(enrollment -> FEEDBACK_ELIGIBLE_STATUSES.contains(enrollment.getStatus()))
                .filter(enrollment -> enrollment.getCourse() != null)
                .toList();

        Map<Long, StudentAssignmentOption> options = new HashMap<>();
        for (Enrollment enrollment : enrollments) {
            assignmentRepository
                    .findByCourseIdAndSectionIgnoreCase(enrollment.getCourse().getId(), enrollment.getSection())
                    .ifPresent(assignment -> {
                        boolean submitted = feedbackRepository
                                .findByAssignmentIdAndStudentId(assignment.getId(), studentId)
                                .isPresent();
                        options.put(assignment.getId(), new StudentAssignmentOption(
                                assignment.getId(),
                                assignment.getCourse().getCode(),
                                assignment.getCourse().getTitle(),
                                assignment.getSection(),
                                assignment.getFaculty().getFullName(),
                                submitted
                        ));
                    });
        }
        return options.values().stream()
                .sorted((a, b) -> a.courseCode().compareToIgnoreCase(b.courseCode()))
                .toList();
    }

    @Transactional
    public void submitFeedback(Long studentId, Long assignmentId, Map<Long, Integer> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            throw new BadRequestException("Please answer all feedback questions");
        }

        CourseInstructorAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assigned course section not found"));

        Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, assignment.getCourse().getId())
                .orElseThrow(() -> new BadRequestException("You are not enrolled in this course"));
        if (!enrollment.getSection().equalsIgnoreCase(assignment.getSection())) {
            throw new BadRequestException("Feedback is only allowed for your enrolled section");
        }

        FacultyFeedback feedback = feedbackRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseGet(FacultyFeedback::new);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        feedback.setAssignment(assignment);
        feedback.setStudent(student);
        FacultyFeedback savedFeedback = feedbackRepository.save(feedback);
        responseRepository.deleteByFeedbackId(savedFeedback.getId());

        List<FeedbackQuestion> questions = getQuestions();
        for (FeedbackQuestion question : questions) {
            Integer rating = ratings.get(question.getId());
            if (rating == null || rating < 1 || rating > 5) {
                throw new BadRequestException("Each question must have a rating from 1 to 5");
            }
            FacultyFeedbackResponse response = new FacultyFeedbackResponse();
            response.setFeedback(savedFeedback);
            response.setQuestion(question);
            response.setRating(rating);
            responseRepository.save(response);
        }
    }

    public record StudentAssignmentOption(Long assignmentId,
                                          String courseCode,
                                          String courseTitle,
                                          String section,
                                          String facultyName,
                                          boolean submitted) {
    }
}
