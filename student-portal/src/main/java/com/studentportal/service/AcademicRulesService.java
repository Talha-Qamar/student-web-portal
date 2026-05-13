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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AcademicRulesService {

    private static final int MAX_SEMESTERS = 8;
    private static final int MAX_CREDITS_PER_SEMESTER = 18;
    private static final int MIN_CREDITS_TO_PROMOTE = 15;
    private static final Set<String> PASSING_GRADES = Set.of("A", "A-", "B+", "B",
            "B-", "C+", "C");
    private static final Set<String> CRITICAL_FAILURES = Set.of("D", "F");

    private final EnrollmentRepository enrollmentRepository;
    private final SemesterProgressRepository semesterProgressRepository;

    public AcademicRulesService(EnrollmentRepository enrollmentRepository,
                                SemesterProgressRepository semesterProgressRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.semesterProgressRepository = semesterProgressRepository;
    }

    @Transactional
    public List<SemesterProgress> ensureSemesterProgress(Student student) {
        Map<Integer, SemesterProgress> progressBySemester = semesterProgressRepository
                .findByStudentIdOrderBySemesterNumber(student.getId()).stream()
                .collect(Collectors.toMap(SemesterProgress::getSemesterNumber, sp -> sp, (a, b) -> a, HashMap::new));

        for (int semester = 1; semester <= MAX_SEMESTERS; semester++) {
            SemesterProgress progress = progressBySemester.get(semester);
            if (progress == null) {
                progress = new SemesterProgress();
                progress.setStudent(student);
                progress.setSemesterNumber(semester);
            }

            if (semester < student.getCurrentSemester()) {
                if (!progress.isFinalized()) {
                    progress.setStatus(SemesterStatus.FINALIZED);
                    if (progress.getFinalizedAt() == null) {
                        progress.setFinalizedAt(LocalDateTime.now());
                    }
                }
            } else if (semester == student.getCurrentSemester()) {
                progress.setStatus(SemesterStatus.ACTIVE);
            }

            progressBySemester.put(semester, progress);
        }

        return semesterProgressRepository.saveAll(progressBySemester.values());
    }

    public void validateRegistration(Student student, Course course) {
        if (course == null) {
            throw new BadRequestException("Course is required");
        }

        ensureSemesterProgress(student);

        int courseSemester = Optional.ofNullable(course.getSemesterNumber()).orElse(student.getCurrentSemester());
        boolean backlog = hasOutstandingRepeat(student, course);

        if (courseSemester > student.getCurrentSemester() && !backlog) {
            throw new BadRequestException("Clear previous semester before registering for " + course.getCode());
        }

        if (courseSemester < student.getCurrentSemester() && !backlog) {
            throw new BadRequestException("Semester " + courseSemester + " is finalized");
        }

        if (isSemesterFinalized(student, courseSemester) && !backlog) {
            throw new BadRequestException("Semester is locked. No changes allowed.");
        }

        if (course.getCreditHours() != null) {
            int currentCredits = getActiveCredits(student.getId());
            if (currentCredits + course.getCreditHours() > MAX_CREDITS_PER_SEMESTER) {
                throw new BadRequestException("Credit limit exceeded (max " + MAX_CREDITS_PER_SEMESTER + " CH)");
            }
        }

        if (course.getPrerequisite() != null && !isPrerequisiteSatisfied(student.getId(), course.getPrerequisite())) {
            throw new BadRequestException("Prerequisite " + course.getPrerequisite().getCode() + " not cleared");
        }

        enforcePromotionRule(student);
    }

    public boolean isSemesterFinalized(Student student, int semesterNumber) {
        return semesterProgressRepository.findByStudentIdAndSemesterNumber(student.getId(), semesterNumber)
                .map(SemesterProgress::isFinalized)
                .orElse(false);
    }

    private int getActiveCredits(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId).stream()
                .filter(enrollment -> EnrollmentStatus.ENROLLED.equals(enrollment.getStatus()))
                .map(Enrollment::getCourse)
                .map(Course::getCreditHours)
                .filter(ch -> ch != null)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private void enforcePromotionRule(Student student) {
        if (student.getCurrentSemester() <= 1) {
            return;
        }
        int previousSemester = student.getCurrentSemester() - 1;
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());

        boolean hasDataForPreviousSemester = enrollments.stream()
                .anyMatch(enrollment -> enrollment.getCourse() != null
                        && enrollment.getCourse().getSemesterNumber() != null
                        && enrollment.getCourse().getSemesterNumber() == previousSemester);

        if (!hasDataForPreviousSemester) {
            return;
        }

        int earnedCredits = enrollments.stream()
                .filter(enrollment -> EnrollmentStatus.COMPLETED.equals(enrollment.getStatus()))
                .filter(enrollment -> enrollment.getCourse() != null)
                .filter(enrollment -> enrollment.getCourse().getSemesterNumber() != null)
                .filter(enrollment -> enrollment.getCourse().getSemesterNumber() == previousSemester)
                .filter(enrollment -> isPassingGrade(enrollment.getGrade()))
                .map(Enrollment::getCourse)
                .map(Course::getCreditHours)
                .filter(ch -> ch != null)
                .mapToInt(Integer::intValue)
                .sum();

        boolean hasCriticalFailure = enrollments.stream()
                .filter(enrollment -> enrollment.getCourse() != null)
                .filter(enrollment -> enrollment.getCourse().getSemesterNumber() != null)
                .filter(enrollment -> enrollment.getCourse().getSemesterNumber() == previousSemester)
                .anyMatch(enrollment -> isCriticalFailure(enrollment.getGrade()));

        if (earnedCredits < MIN_CREDITS_TO_PROMOTE || hasCriticalFailure) {
            throw new BadRequestException("Clear previous semester before proceeding");
        }
    }

    public boolean isPrerequisiteSatisfied(Long studentId, Course prerequisite) {
        return enrollmentRepository.findByStudentId(studentId).stream()
                .filter(enrollment -> enrollment.getCourse() != null)
                .filter(enrollment -> prerequisite.getId().equals(enrollment.getCourse().getId()))
                .anyMatch(enrollment -> isPassingGrade(enrollment.getGrade()));
    }

    public boolean hasOutstandingRepeat(Student student, Course course) {
        return enrollmentRepository.findByStudentId(student.getId()).stream()
                .filter(enrollment -> enrollment.getCourse() != null)
                .filter(enrollment -> enrollment.getCourse().getId().equals(course.getId()))
                .anyMatch(enrollment -> isCriticalFailure(enrollment.getGrade()) || enrollment.isRepeatRequired());
    }

    public boolean canDrop(Enrollment enrollment) {
        if (enrollment.getGrade() != null && !enrollment.getGrade().isBlank()) {
            return false;
        }
        Course course = enrollment.getCourse();
        if (course == null || course.getSemesterNumber() == null) {
            return true;
        }
        Student student = enrollment.getStudent();
        return !isSemesterFinalized(student, course.getSemesterNumber());
    }

    public boolean isPassingGrade(String grade) {
        if (grade == null) {
            return false;
        }
        return PASSING_GRADES.contains(grade.toUpperCase());
    }

    public boolean isCriticalFailure(String grade) {
        if (grade == null) {
            return false;
        }
        return CRITICAL_FAILURES.contains(grade.toUpperCase());
    }

    public void refreshRepeatFlags(Long studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        List<Enrollment> dirty = enrollments.stream()
                .filter(enrollment -> enrollment.isRepeatRequired() != isCriticalFailure(enrollment.getGrade()))
                .peek(enrollment -> enrollment.setRepeatRequired(isCriticalFailure(enrollment.getGrade())))
                .collect(Collectors.toList());
        if (!dirty.isEmpty()) {
            enrollmentRepository.saveAll(dirty);
        }
    }

    public Map<Integer, SemesterProgress> progressBySemester(Long studentId) {
        return semesterProgressRepository.findByStudentIdOrderBySemesterNumber(studentId).stream()
                .collect(Collectors.toMap(SemesterProgress::getSemesterNumber, sp -> sp));
    }

    public void updateSummaries(Long studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        Map<Integer, List<Enrollment>> bySemester = enrollments.stream()
                .filter(enrollment -> enrollment.getCourse() != null)
                .filter(enrollment -> enrollment.getCourse().getSemesterNumber() != null)
                .collect(Collectors.groupingBy(enrollment -> enrollment.getCourse().getSemesterNumber()));

        List<SemesterProgress> existing = semesterProgressRepository.findByStudentIdOrderBySemesterNumber(studentId);
        for (SemesterProgress progress : existing) {
            List<Enrollment> semesterEnrollments = bySemester.getOrDefault(progress.getSemesterNumber(), List.of());
            int credits = semesterEnrollments.stream()
                    .filter(enrollment -> EnrollmentStatus.COMPLETED.equals(enrollment.getStatus()))
                    .filter(enrollment -> isPassingGrade(enrollment.getGrade()))
                    .map(Enrollment::getCourse)
                    .map(Course::getCreditHours)
                    .filter(ch -> ch != null)
                    .mapToInt(Integer::intValue)
                    .sum();
            double gpa = calculateGpa(semesterEnrollments);
            progress.setEarnedCredits(credits);
            progress.setSemesterGpa(gpa);
        }
        semesterProgressRepository.saveAll(existing);
    }

    private double calculateGpa(Collection<Enrollment> enrollments) {
        double totalPoints = 0.0;
        int totalCredits = 0;
        for (Enrollment enrollment : enrollments) {
            Integer creditHours = enrollment.getCourse() != null ? enrollment.getCourse().getCreditHours() : null;
            if (creditHours == null) {
                continue;
            }
            if (enrollment.getGrade() == null) {
                continue;
            }
            Double points = gradeToPoints(enrollment.getGrade());
            if (points == null) {
                continue;
            }
            totalPoints += points * creditHours;
            totalCredits += creditHours;
        }
        if (totalCredits == 0) {
            return 0.0;
        }
        return Math.round((totalPoints / totalCredits) * 100.0) / 100.0;
    }

    private Double gradeToPoints(String grade) {
        if (grade == null) {
            return null;
        }
        return switch (grade.toUpperCase()) {
            case "A" -> 4.0;
            case "A-" -> 3.7;
            case "B+" -> 3.4;
            case "B" -> 3.0;
            case "B-" -> 2.7;
            case "C+" -> 2.4;
            case "C" -> 2.0;
            case "C-" -> 1.7;
            case "D" -> 1.0;
            case "F" -> 0.0;
            default -> null;
        };
    }
}
