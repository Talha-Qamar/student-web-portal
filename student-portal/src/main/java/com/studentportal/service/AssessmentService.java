package com.studentportal.service;

import com.studentportal.dto.AssessmentCourseDto;
import com.studentportal.dto.AssessmentItemDto;
import com.studentportal.model.AssessmentCategory;
import com.studentportal.model.AssessmentRecord;
import com.studentportal.model.Course;
import com.studentportal.model.Enrollment;
import com.studentportal.model.EnrollmentStatus;
import com.studentportal.repository.AssessmentRecordRepository;
import com.studentportal.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AssessmentService {

    private static final Map<AssessmentCategory, Double> DEFAULT_ABSOLUTES = Map.of(
            AssessmentCategory.QUIZ, 10d,
            AssessmentCategory.ASSIGNMENT, 10d,
            AssessmentCategory.PROJECT, 10d,
            AssessmentCategory.SESSIONAL1, 15d,
            AssessmentCategory.SESSIONAL2, 15d,
            AssessmentCategory.FINAL, 40d
    );

    private final AssessmentRecordRepository assessmentRecordRepository;
    private final EnrollmentRepository enrollmentRepository;

    public AssessmentService(AssessmentRecordRepository assessmentRecordRepository,
                             EnrollmentRepository enrollmentRepository) {
        this.assessmentRecordRepository = assessmentRecordRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public List<Integer> getTrackedSemesters(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId).stream()
                .filter(enrollment -> enrollment.getCourse() != null && enrollment.getCourse().getSemesterNumber() != null)
                .filter(enrollment -> !EnrollmentStatus.DROPPED.equals(enrollment.getStatus()))
                .map(enrollment -> enrollment.getCourse().getSemesterNumber())
                .distinct()
                .sorted()
                .toList();
    }

    public List<AssessmentCourseDto> getAssessmentsForSemester(Long studentId, Integer semesterNumber) {
        List<Enrollment> semesterEnrollments = enrollmentRepository.findByStudentId(studentId).stream()
                .filter(enrollment -> enrollment.getCourse() != null && enrollment.getCourse().getSemesterNumber() != null)
                .filter(enrollment -> enrollment.getCourse().getSemesterNumber().equals(semesterNumber))
                .filter(enrollment -> !EnrollmentStatus.DROPPED.equals(enrollment.getStatus()))
                .sorted((a, b) -> a.getCourse().getCode().compareToIgnoreCase(b.getCourse().getCode()))
                .toList();

        Map<Long, List<AssessmentRecord>> recordsByCourse = assessmentRecordRepository
                .findByStudentIdAndCourseSemesterNumber(studentId, semesterNumber)
                .stream()
                .collect(Collectors.groupingBy(record -> record.getCourse().getId()));

        return semesterEnrollments.stream()
                .map(enrollment -> buildCourseDto(enrollment, recordsByCourse.getOrDefault(enrollment.getCourse().getId(), List.of())))
                .toList();
    }

    private AssessmentCourseDto buildCourseDto(Enrollment enrollment, List<AssessmentRecord> records) {
        Course course = enrollment.getCourse();
        Map<AssessmentCategory, List<AssessmentItemDto>> breakdown = new EnumMap<>(AssessmentCategory.class);
        for (AssessmentCategory category : AssessmentCategory.values()) {
            breakdown.put(category, List.of());
        }

        double earnedAbsolute = 0;
        double maxAbsolute = 0;

        Map<AssessmentCategory, List<AssessmentItemDto>> populated = records.stream()
                .collect(Collectors.groupingBy(AssessmentRecord::getCategory, () -> new EnumMap<>(AssessmentCategory.class), Collectors.toList()))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(record -> {
                            double weight = safeWeight(record);
                            double absoluteEarned = weight * safeRatio(record);
                            return new AssessmentItemDto(
                                    record.getCategory(),
                                    record.getTitle(),
                                    safeNumber(record.getObtainedMarks()),
                                    safeNumber(record.getTotalMarks()),
                                    weight,
                                    Math.round(absoluteEarned * 10.0) / 10.0
                            );
                        }).toList(),
                        (left, right) -> right,
                        () -> new EnumMap<>(AssessmentCategory.class)
                ));

        for (AssessmentCategory category : AssessmentCategory.values()) {
            List<AssessmentItemDto> items = populated.getOrDefault(category, List.of());
            breakdown.put(category, items);
            for (AssessmentItemDto item : items) {
                earnedAbsolute += item.getAbsoluteEarned();
                maxAbsolute += item.getAbsoluteWeight();
            }
        }

        if (maxAbsolute == 0) {
            maxAbsolute = 100;
        }

        return new AssessmentCourseDto(
                course.getId(),
                course.getCode(),
                course.getTitle(),
                Math.round(earnedAbsolute * 10.0) / 10.0,
                maxAbsolute,
                breakdown
        );
    }

    private double safeWeight(AssessmentRecord record) {
        return record.getAbsoluteWeight() != null
                ? record.getAbsoluteWeight()
                : DEFAULT_ABSOLUTES.getOrDefault(record.getCategory(), 0d);
    }

    private double safeRatio(AssessmentRecord record) {
        double obtained = safeNumber(record.getObtainedMarks());
        double total = safeNumber(record.getTotalMarks());
        if (total <= 0) {
            return 0;
        }
        return obtained / total;
    }

    private double safeNumber(Double value) {
        return value == null ? 0 : value;
    }
}
