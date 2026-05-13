package com.studentportal.repository;

import com.studentportal.model.SemesterProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SemesterProgressRepository extends JpaRepository<SemesterProgress, Long> {
    List<SemesterProgress> findByStudentIdOrderBySemesterNumber(Long studentId);
    Optional<SemesterProgress> findByStudentIdAndSemesterNumber(Long studentId, Integer semesterNumber);
}
