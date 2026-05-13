package com.studentportal.service;

import com.studentportal.exception.BadRequestException;
import com.studentportal.exception.ResourceNotFoundException;
import com.studentportal.model.AssessmentCategory;
import com.studentportal.model.AssessmentRecord;
import com.studentportal.model.AttendanceRecord;
import com.studentportal.model.AttendanceStatus;
import com.studentportal.model.CourseInstructorAssignment;
import com.studentportal.model.Enrollment;
import com.studentportal.model.EnrollmentStatus;
import com.studentportal.model.FacultyFeedback;
import com.studentportal.model.FacultyFeedbackResponse;
import com.studentportal.repository.AssessmentRecordRepository;
import com.studentportal.repository.AttendanceRecordRepository;
import com.studentportal.repository.CourseInstructorAssignmentRepository;
import com.studentportal.repository.CourseRepository;
import com.studentportal.repository.EnrollmentRepository;
import com.studentportal.repository.FacultyFeedbackRepository;
import com.studentportal.repository.FacultyFeedbackResponseRepository;
import com.studentportal.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FacultyWorkflowService {

    private static final Set<EnrollmentStatus> ACTIVE_ENROLLMENT_STATUSES =
            EnumSet.of(EnrollmentStatus.ENROLLED, EnrollmentStatus.COMPLETED);

    private final CourseInstructorAssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AssessmentRecordRepository assessmentRecordRepository;
    private final FacultyFeedbackRepository facultyFeedbackRepository;
    private final FacultyFeedbackResponseRepository feedbackResponseRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    public FacultyWorkflowService(CourseInstructorAssignmentRepository assignmentRepository,
                                  EnrollmentRepository enrollmentRepository,
                                  AttendanceRecordRepository attendanceRecordRepository,
                                  AssessmentRecordRepository assessmentRecordRepository,
                                  FacultyFeedbackRepository facultyFeedbackRepository,
                                  FacultyFeedbackResponseRepository feedbackResponseRepository,
                                  StudentRepository studentRepository,
                                  CourseRepository courseRepository) {
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.assessmentRecordRepository = assessmentRecordRepository;
        this.facultyFeedbackRepository = facultyFeedbackRepository;
        this.feedbackResponseRepository = feedbackResponseRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
    }

    public List<CourseInstructorAssignment> getAssignmentsForFaculty(Long facultyId) {
        return assignmentRepository.findByFacultyIdOrderByCourseCodeAscSectionAsc(facultyId);
    }

    public List<Enrollment> getRoster(Long facultyId, Long assignmentId) {
        CourseInstructorAssignment assignment = getAuthorizedAssignment(facultyId, assignmentId);
        return enrollmentRepository.findByCourseIdAndSectionIgnoreCaseAndStatusIn(
                assignment.getCourse().getId(),
                assignment.getSection(),
                ACTIVE_ENROLLMENT_STATUSES
        ).stream().sorted((a, b) -> {
            String aRoll = a.getStudent().getRollNumber();
            String bRoll = b.getStudent().getRollNumber();
            if (aRoll == null && bRoll == null) {
                return a.getStudent().getFullName().compareToIgnoreCase(b.getStudent().getFullName());
            }
            if (aRoll == null) {
                return 1;
            }
            if (bRoll == null) {
                return -1;
            }
            return aRoll.compareToIgnoreCase(bRoll);
        }).toList();
    }

    public List<AttendanceRecord> getAttendanceRecordsForAssignment(Long facultyId, Long assignmentId) {
        CourseInstructorAssignment assignment = getAuthorizedAssignment(facultyId, assignmentId);
        List<Long> rosterStudentIds = getRoster(facultyId, assignmentId).stream()
            .map(enrollment -> enrollment.getStudent().getId())
            .toList();
        if (rosterStudentIds.isEmpty()) {
            return List.of();
        }
        return attendanceRecordRepository.findByCourseIdAndStudentIdInOrderByAttendanceDateAscStudentRollNumberAsc(
            assignment.getCourse().getId(),
            rosterStudentIds);
    }

    @Transactional
    public int uploadAttendance(Long facultyId,
                                Long assignmentId,
                                LocalDate attendanceDate,
                                Map<Long, AttendanceStatus> byStudent) {
        CourseInstructorAssignment assignment = getAuthorizedAssignment(facultyId, assignmentId);
        List<Enrollment> roster = getRoster(facultyId, assignmentId);
        Map<Long, Enrollment> enrollments = roster.stream()
                .collect(Collectors.toMap(e -> e.getStudent().getId(), e -> e));

        int written = 0;

        for (Map.Entry<Long, AttendanceStatus> entry : byStudent.entrySet()) {
            Enrollment enrollment = enrollments.get(entry.getKey());
            if (enrollment == null) {
                continue;
            }
            AttendanceRecord record = attendanceRecordRepository
                    .findByStudentIdAndCourseIdAndAttendanceDate(entry.getKey(), assignment.getCourse().getId(), attendanceDate)
                    .orElseGet(AttendanceRecord::new);
            record.setStudent(enrollment.getStudent());
            record.setCourse(assignment.getCourse());
            record.setAttendanceDate(attendanceDate);
            record.setStatus(entry.getValue());
            attendanceRecordRepository.save(record);
            written++;
        }

        return written;
    }

    @Transactional
    public void uploadGrades(Long facultyId,
                             Long assignmentId,
                             Map<Long, String> byStudent,
                             String assessmentTitle,
                             AssessmentCategory assessmentCategory,
                             Double totalMarks,
                             Double absoluteWeight) {
        CourseInstructorAssignment assignment = getAuthorizedAssignment(facultyId, assignmentId);
        List<Enrollment> roster = getRoster(facultyId, assignmentId);
        Map<Long, Enrollment> enrollments = roster.stream()
                .collect(Collectors.toMap(e -> e.getStudent().getId(), e -> e));

        for (Map.Entry<Long, String> entry : byStudent.entrySet()) {
            Enrollment enrollment = enrollments.get(entry.getKey());
            if (enrollment == null || entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }

            String grade = entry.getValue().trim().toUpperCase();
            enrollment.setGrade(grade);
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            enrollmentRepository.save(enrollment);

            if (assessmentCategory != null && totalMarks != null && absoluteWeight != null) {
                double obtainedMarks = gradeToPercentage(grade) * totalMarks;
                saveAssessmentRecord(enrollment.getStudent().getId(), assignment.getCourse().getId(), assessmentCategory,
                        (assessmentTitle == null || assessmentTitle.isBlank())
                                ? assessmentCategory.getLabel() + " Upload"
                                : assessmentTitle.trim(),
                        Math.round(obtainedMarks * 100.0) / 100.0,
                        totalMarks,
                        absoluteWeight);
            }
        }
    }

    @Transactional
    public void saveAssessmentScore(Long facultyId,
                                    Long assignmentId,
                                    Long studentId,
                                    AssessmentCategory assessmentCategory,
                                    String assessmentTitle,
                                    Double obtainedMarks,
                                    Double totalMarks,
                                    Double absoluteWeight) {
        CourseInstructorAssignment assignment = getAuthorizedAssignment(facultyId, assignmentId);
        List<Enrollment> roster = getRoster(facultyId, assignmentId);
        boolean enrolled = roster.stream().anyMatch(enrollment -> enrollment.getStudent().getId().equals(studentId));
        if (!enrolled) {
            return;
        }

        if (assessmentCategory == null || assessmentTitle == null || assessmentTitle.isBlank()) {
            throw new BadRequestException("Assessment details are required");
        }

        saveAssessmentRecord(studentId, assignment.getCourse().getId(), assessmentCategory, assessmentTitle.trim(),
                obtainedMarks == null ? 0.0 : obtainedMarks,
                totalMarks == null ? 0.0 : totalMarks,
                absoluteWeight == null ? 0.0 : absoluteWeight);
    }

    private void saveAssessmentRecord(Long studentId,
                                      Long courseId,
                                      AssessmentCategory assessmentCategory,
                                      String assessmentTitle,
                                      Double obtainedMarks,
                                      Double totalMarks,
                                      Double absoluteWeight) {
        AssessmentRecord record = assessmentRecordRepository
                .findByStudentIdAndCourseIdAndCategoryAndTitleIgnoreCase(studentId, courseId, assessmentCategory, assessmentTitle)
                .orElseGet(AssessmentRecord::new);
        record.setStudent(studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found")));
        record.setCourse(courseRepository.findById(courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found")));
        record.setCategory(assessmentCategory);
        record.setTitle(assessmentTitle);
        record.setObtainedMarks(Math.round(obtainedMarks * 100.0) / 100.0);
        record.setTotalMarks(totalMarks);
        record.setAbsoluteWeight(absoluteWeight);
        assessmentRecordRepository.save(record);
    }

    public List<FeedbackInsightRow> getFeedbackInsights(Long facultyId) {
        List<FacultyFeedback> feedback = facultyFeedbackRepository.findByAssignmentFacultyId(facultyId);
        if (feedback.isEmpty()) {
            return List.of();
        }

        Map<Long, CourseInstructorAssignment> assignments = new HashMap<>();
        for (FacultyFeedback item : feedback) {
            assignments.put(item.getAssignment().getId(), item.getAssignment());
        }

        List<FacultyFeedbackResponse> responses = feedbackResponseRepository.findByFeedbackAssignmentFacultyId(facultyId);
        Map<Long, List<FacultyFeedbackResponse>> byAssignment = responses.stream()
                .collect(Collectors.groupingBy(response -> response.getFeedback().getAssignment().getId()));

        return assignments.values().stream().map(assignment -> {
            List<FacultyFeedbackResponse> assignmentResponses = byAssignment.getOrDefault(assignment.getId(), List.of());
            Map<String, Double> questionAverages = assignmentResponses.stream()
                    .collect(Collectors.groupingBy(
                            response -> response.getQuestion().getPrompt(),
                            Collectors.averagingDouble(FacultyFeedbackResponse::getRating)
                    ));

            double overall = assignmentResponses.stream()
                    .mapToInt(FacultyFeedbackResponse::getRating)
                    .average()
                    .orElse(0.0);

            return new FeedbackInsightRow(
                    assignment.getId(),
                    assignment.getCourse().getCode(),
                    assignment.getCourse().getTitle(),
                    assignment.getSection(),
                    feedback.stream().filter(item -> item.getAssignment().getId().equals(assignment.getId())).count(),
                    Math.round(overall * 100.0) / 100.0,
                    questionAverages
            );
        }).sorted((a, b) -> a.courseCode().compareToIgnoreCase(b.courseCode())).toList();
    }

    private CourseInstructorAssignment getAuthorizedAssignment(Long facultyId, Long assignmentId) {
        CourseInstructorAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        if (!assignment.getFaculty().getId().equals(facultyId)) {
            throw new BadRequestException("You are not assigned to this course section");
        }
        return assignment;
    }

    /**
     * Get distinct assessments (category + title) for a given course.
     * Used to populate existing assessment columns when loading the grade form.
     */
    public List<AssessmentColumnInfo> getExistingAssessments(Long courseId) {
        var distinctRecords = assessmentRecordRepository.findDistinctAssessmentsByCourseSorted(courseId);
        return distinctRecords.stream()
                .filter(obj -> obj instanceof java.util.Map)
                .map(obj -> {
                    @SuppressWarnings("unchecked")
                    var map = (java.util.Map<String, Object>) obj;
                    return new AssessmentColumnInfo(
                            (AssessmentCategory) map.get("category"),
                            (String) map.get("title"),
                            ((Number) map.getOrDefault("totalMarks", 10.0)).doubleValue(),
                            ((Number) map.getOrDefault("absoluteWeight", 2.0)).doubleValue()
                    );
                })
                .toList();
    }

    /**
     * Get all assessment values for a specific course and student.
     */
    public Map<String, Double> getStudentAssessmentMarks(Long studentId, Long courseId) {
        return assessmentRecordRepository.findByCourseId(courseId).stream()
                .filter(ar -> ar.getStudent().getId().equals(studentId))
                .collect(Collectors.toMap(
                        ar -> ar.getCategory() + "|" + ar.getTitle(),
                        AssessmentRecord::getObtainedMarks,
                        (v1, v2) -> v1
                ));
    }

    private double gradeToPercentage(String grade) {
        return switch (grade) {
            case "A" -> 0.95;
            case "A-" -> 0.90;
            case "B+" -> 0.85;
            case "B" -> 0.80;
            case "B-" -> 0.75;
            case "C+" -> 0.70;
            case "C" -> 0.65;
            case "C-" -> 0.60;
            case "D" -> 0.55;
            default -> 0.40;
        };
    }

    public record AssessmentColumnInfo(AssessmentCategory category, String title, Double totalMarks, Double absoluteWeight) {
    }

    public record FeedbackInsightRow(Long assignmentId,
                                     String courseCode,
                                     String courseTitle,
                                     String section,
                                     long submissions,
                                     double overallRating,
                                     Map<String, Double> questionAverages) {
    }
}
