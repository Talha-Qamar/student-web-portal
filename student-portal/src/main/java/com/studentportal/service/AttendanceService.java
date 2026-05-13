package com.studentportal.service;

import com.studentportal.dto.AttendanceCourseDto;
import com.studentportal.dto.AttendanceEntryDto;
import com.studentportal.model.AttendanceRecord;
import com.studentportal.model.AttendanceStatus;
import com.studentportal.model.Course;
import com.studentportal.model.Enrollment;
import com.studentportal.model.EnrollmentStatus;
import com.studentportal.repository.AttendanceRecordRepository;
import com.studentportal.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;

import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EnrollmentRepository enrollmentRepository;

    public AttendanceService(AttendanceRecordRepository attendanceRecordRepository,
                             EnrollmentRepository enrollmentRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
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

    public List<AttendanceCourseDto> getAttendanceForSemester(Long studentId, Integer semesterNumber) {
        List<Enrollment> semesterEnrollments = enrollmentRepository.findByStudentId(studentId).stream()
                .filter(enrollment -> enrollment.getCourse() != null && enrollment.getCourse().getSemesterNumber() != null)
                .filter(enrollment -> enrollment.getCourse().getSemesterNumber().equals(semesterNumber))
                .filter(enrollment -> !EnrollmentStatus.DROPPED.equals(enrollment.getStatus()))
                .sorted(Comparator.comparing(enrollment -> enrollment.getCourse().getCode()))
                .toList();

        Map<Long, List<AttendanceRecord>> recordsByCourse = attendanceRecordRepository
                .findByStudentIdAndCourseSemesterNumberOrderByAttendanceDateAsc(studentId, semesterNumber)
                .stream()
                .collect(Collectors.groupingBy(record -> record.getCourse().getId()));

        return semesterEnrollments.stream()
                .map(enrollment -> buildCourseDto(enrollment, recordsByCourse.getOrDefault(enrollment.getCourse().getId(), List.of())))
                .toList();
    }

    private AttendanceCourseDto buildCourseDto(Enrollment enrollment, List<AttendanceRecord> records) {
        Course course = enrollment.getCourse();
        int totalSessions = records.size();
        long presentSessions = records.stream()
                .filter(record -> AttendanceStatus.PRESENT.equals(record.getStatus()))
                .count();
        double percentage = totalSessions == 0 ? 0 : (presentSessions * 100.0) / totalSessions;

        List<AttendanceEntryDto> entries = records.stream()
                .map(record -> new AttendanceEntryDto(
                        record.getAttendanceDate(),
                        record.getAttendanceDate().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                        record.getStatus()))
                .toList();

        return new AttendanceCourseDto(
                course.getId(),
                course.getCode(),
                course.getTitle(),
                Math.round(percentage * 10.0) / 10.0,
                totalSessions,
                (int) presentSessions,
                entries
        );
    }
}
