package com.studentportal.dto;

import com.studentportal.model.AttendanceStatus;

import java.time.LocalDate;

public class AttendanceEntryDto {

    private final LocalDate date;
    private final String day;
    private final AttendanceStatus status;

    public AttendanceEntryDto(LocalDate date, String day, AttendanceStatus status) {
        this.date = date;
        this.day = day;
        this.status = status;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDay() {
        return day;
    }

    public AttendanceStatus getStatus() {
        return status;
    }
}
