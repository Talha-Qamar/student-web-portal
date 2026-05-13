package com.studentportal.dto;

import java.util.List;

public class TranscriptResponse {

    private final String studentName;
    private final String major;
    private final Integer enrollmentYear;
    private final Double overallGpa;
    private final Integer totalCredits;
    private final List<TranscriptCourseDto> courses;
    private final List<SemesterTranscriptBlock> semesters;

    public TranscriptResponse(String studentName, String major, Integer enrollmentYear,
                              Double overallGpa, Integer totalCredits,
                              List<TranscriptCourseDto> courses,
                              List<SemesterTranscriptBlock> semesters) {
        this.studentName = studentName;
        this.major = major;
        this.enrollmentYear = enrollmentYear;
        this.overallGpa = overallGpa;
        this.totalCredits = totalCredits;
        this.courses = courses;
        this.semesters = semesters;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getMajor() {
        return major;
    }

    public Integer getEnrollmentYear() {
        return enrollmentYear;
    }

    public Double getOverallGpa() {
        return overallGpa;
    }

    public Integer getTotalCredits() {
        return totalCredits;
    }

    public List<TranscriptCourseDto> getCourses() {
        return courses;
    }

    public List<SemesterTranscriptBlock> getSemesters() {
        return semesters;
    }
}
