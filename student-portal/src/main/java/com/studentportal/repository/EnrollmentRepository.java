package com.studentportal.repository;

import com.studentportal.model.Enrollment;
import com.studentportal.model.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudentId(Long studentId);
    List<Enrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status);
    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
    List<Enrollment> findByCourseIdAndSectionIgnoreCaseAndStatusIn(Long courseId,
                                                                    String section,
                                                                    Collection<EnrollmentStatus> statuses);
}
