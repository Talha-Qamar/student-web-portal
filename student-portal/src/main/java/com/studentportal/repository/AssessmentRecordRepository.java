package com.studentportal.repository;

import com.studentportal.model.AssessmentRecord;
import com.studentportal.model.AssessmentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssessmentRecordRepository extends JpaRepository<AssessmentRecord, Long> {
    List<AssessmentRecord> findByStudentIdAndCourseSemesterNumber(Long studentId, Integer semesterNumber);
    Optional<AssessmentRecord> findByStudentIdAndCourseIdAndCategoryAndTitleIgnoreCase(Long studentId,
                                                                                     Long courseId,
                                                                                     AssessmentCategory category,
                                                                                     String title);
    boolean existsByStudentId(Long studentId);

    /**
     * Get all distinct assessments for a course (one record per category/title combo, any student).
     */
    @Query("SELECT DISTINCT NEW map(ar.category as category, ar.title as title, ar.totalMarks as totalMarks, ar.absoluteWeight as absoluteWeight) " +
           "FROM AssessmentRecord ar " +
           "WHERE ar.course.id = :courseId " +
           "ORDER BY ar.category, ar.title")
    List<?> findDistinctAssessmentsByCourseSorted(@Param("courseId") Long courseId);

    /**
     * Get all assessment records for a specific course.
     */
    List<AssessmentRecord> findByCourseId(Long courseId);
}
