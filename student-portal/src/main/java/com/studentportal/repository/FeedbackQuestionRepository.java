package com.studentportal.repository;

import com.studentportal.model.FeedbackQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackQuestionRepository extends JpaRepository<FeedbackQuestion, Long> {
    List<FeedbackQuestion> findByActiveTrueOrderBySortOrderAsc();
}
