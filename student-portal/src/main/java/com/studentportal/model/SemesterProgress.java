package com.studentportal.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "semester_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "semester_number"}))
public class SemesterProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "semester_number", nullable = false)
    private Integer semesterNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SemesterStatus status = SemesterStatus.ACTIVE;

    @Column(name = "earned_credits")
    private Integer earnedCredits = 0;

    @Column(name = "semester_gpa")
    private Double semesterGpa;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    public Long getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Integer getSemesterNumber() {
        return semesterNumber;
    }

    public void setSemesterNumber(Integer semesterNumber) {
        this.semesterNumber = semesterNumber;
    }

    public SemesterStatus getStatus() {
        return status;
    }

    public void setStatus(SemesterStatus status) {
        this.status = status;
    }

    public Integer getEarnedCredits() {
        return earnedCredits;
    }

    public void setEarnedCredits(Integer earnedCredits) {
        this.earnedCredits = earnedCredits;
    }

    public Double getSemesterGpa() {
        return semesterGpa;
    }

    public void setSemesterGpa(Double semesterGpa) {
        this.semesterGpa = semesterGpa;
    }

    public LocalDateTime getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(LocalDateTime finalizedAt) {
        this.finalizedAt = finalizedAt;
    }

    public boolean isFinalized() {
        return SemesterStatus.FINALIZED.equals(status);
    }
}
