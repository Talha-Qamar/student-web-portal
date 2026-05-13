package com.studentportal.dto;

import jakarta.validation.constraints.NotNull;

public class RegisterCourseRequest {

    @NotNull
    private Long studentId;

    @NotNull
    private Long courseId;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }
}
