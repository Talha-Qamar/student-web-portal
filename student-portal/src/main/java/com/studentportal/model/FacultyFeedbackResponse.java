package com.studentportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "faculty_feedback_responses")
public class FacultyFeedbackResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "feedback_id")
    private FacultyFeedback feedback;

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id")
    private FeedbackQuestion question;

    @Column(nullable = false)
    private Integer rating;

    public Long getId() {
        return id;
    }

    public FacultyFeedback getFeedback() {
        return feedback;
    }

    public void setFeedback(FacultyFeedback feedback) {
        this.feedback = feedback;
    }

    public FeedbackQuestion getQuestion() {
        return question;
    }

    public void setQuestion(FeedbackQuestion question) {
        this.question = question;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }
}
