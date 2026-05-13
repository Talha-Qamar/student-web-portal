package com.studentportal.repository;

import com.studentportal.model.FacultyFeedbackResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacultyFeedbackResponseRepository extends JpaRepository<FacultyFeedbackResponse, Long> {
    List<FacultyFeedbackResponse> findByFeedbackAssignmentFacultyId(Long facultyId);
    List<FacultyFeedbackResponse> findByFeedbackId(Long feedbackId);
    void deleteByFeedbackId(Long feedbackId);
}
