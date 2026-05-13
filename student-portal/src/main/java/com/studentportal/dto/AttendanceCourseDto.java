package com.studentportal.dto;

import java.util.List;

public class AttendanceCourseDto {

    private final Long courseId;
    private final String code;
    private final String title;
    private final double attendancePercentage;
    private final int totalSessions;
    private final int presentSessions;
    private final List<AttendanceEntryDto> entries;

    public AttendanceCourseDto(Long courseId,
                               String code,
                               String title,
                               double attendancePercentage,
                               int totalSessions,
                               int presentSessions,
                               List<AttendanceEntryDto> entries) {
        this.courseId = courseId;
        this.code = code;
        this.title = title;
        this.attendancePercentage = attendancePercentage;
        this.totalSessions = totalSessions;
        this.presentSessions = presentSessions;
        this.entries = entries;
    }

    public Long getCourseId() {
        return courseId;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public int getTotalSessions() {
        return totalSessions;
    }

    public int getPresentSessions() {
        return presentSessions;
    }

    public List<AttendanceEntryDto> getEntries() {
        return entries;
    }
}
