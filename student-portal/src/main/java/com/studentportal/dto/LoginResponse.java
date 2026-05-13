package com.studentportal.dto;

import com.studentportal.model.UserRole;

public class LoginResponse {

    private final Long userId;
    private final UserRole role;
    private final Long studentId;
    private final String fullName;
    private final String email;
    private final String major;
    private final Integer enrollmentYear;
    private final Double gpa;
    private final String sessionToken;
    private final Integer currentSemester;

    public LoginResponse(Long userId, UserRole role, Long studentId, String fullName, String email,
                         String major, Integer enrollmentYear, Double gpa,
                         String sessionToken, Integer currentSemester) {
        this.userId = userId;
        this.role = role;
        this.studentId = studentId;
        this.fullName = fullName;
        this.email = email;
        this.major = major;
        this.enrollmentYear = enrollmentYear;
        this.gpa = gpa;
        this.sessionToken = sessionToken;
        this.currentSemester = currentSemester;
    }

    public Long getUserId() {
        return userId;
    }

    public UserRole getRole() {
        return role;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getMajor() {
        return major;
    }

    public Integer getEnrollmentYear() {
        return enrollmentYear;
    }

    public Double getGpa() {
        return gpa;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public Integer getCurrentSemester() {
        return currentSemester;
    }
}
