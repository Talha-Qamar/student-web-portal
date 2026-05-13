package com.studentportal.repository;

import com.studentportal.model.CourseInstructorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseInstructorAssignmentRepository extends JpaRepository<CourseInstructorAssignment, Long> {
    List<CourseInstructorAssignment> findByFacultyIdOrderByCourseCodeAscSectionAsc(Long facultyId);
    List<CourseInstructorAssignment> findByCourseIdOrderBySectionAsc(Long courseId);
    List<CourseInstructorAssignment> findAllByOrderByTermDescCourseCodeAscSectionAsc();
    Optional<CourseInstructorAssignment> findByCourseIdAndSectionIgnoreCase(Long courseId, String section);
}
