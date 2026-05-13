package com.studentportal.dto;

public class TranscriptCourseDto {

    private final String code;
    private final String title;
    private final String term;
    private final Integer creditHours;
    private final String grade;
    private final String status;
    private final Double gradePoints;
    private final Integer semesterNumber;
    private final boolean repeatRequired;

    public TranscriptCourseDto(String code, String title, String term,
                               Integer creditHours, String grade,
                               String status, Double gradePoints,
                               Integer semesterNumber, boolean repeatRequired) {
        this.code = code;
        this.title = title;
        this.term = term;
        this.creditHours = creditHours;
        this.grade = grade;
        this.status = status;
        this.gradePoints = gradePoints;
        this.semesterNumber = semesterNumber;
        this.repeatRequired = repeatRequired;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getTerm() {
        return term;
    }

    public Integer getCreditHours() {
        return creditHours;
    }

    public String getGrade() {
        return grade;
    }

    public String getStatus() {
        return status;
    }

    public Double getGradePoints() {
        return gradePoints;
    }

    public Integer getSemesterNumber() {
        return semesterNumber;
    }

    public boolean isRepeatRequired() {
        return repeatRequired;
    }
}
