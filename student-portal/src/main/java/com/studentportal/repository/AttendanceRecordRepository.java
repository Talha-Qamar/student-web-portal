package com.studentportal.repository;

import com.studentportal.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findByStudentIdAndCourseSemesterNumberOrderByAttendanceDateAsc(Long studentId, Integer semesterNumber);
    List<AttendanceRecord> findByCourseIdOrderByAttendanceDateAscStudentRollNumberAsc(Long courseId);
    List<AttendanceRecord> findByCourseIdAndStudentIdInOrderByAttendanceDateAscStudentRollNumberAsc(Long courseId,
                                                                                                      List<Long> studentIds);
    boolean existsByStudentId(Long studentId);
    Optional<AttendanceRecord> findByStudentIdAndCourseIdAndAttendanceDate(Long studentId, Long courseId, LocalDate attendanceDate);
}
