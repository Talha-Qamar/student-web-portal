package com.studentportal.repository;

import com.studentportal.model.FacultyFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacultyFeedbackRepository extends JpaRepository<FacultyFeedback, Long> {
    Optional<FacultyFeedback> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);
    List<FacultyFeedback> findByAssignmentFacultyId(Long facultyId);
    List<FacultyFeedback> findByAssignmentId(Long assignmentId);
}
