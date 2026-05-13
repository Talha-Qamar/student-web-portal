package com.studentportal.dto;

import java.util.List;

public class SemesterTranscriptBlock {

    private final int semesterNumber;
    private final boolean finalized;
    private final double semesterGpa;
    private final int creditsEarned;
    private final List<TranscriptCourseDto> courses;

    public SemesterTranscriptBlock(int semesterNumber,
                                   boolean finalized,
                                   double semesterGpa,
                                   int creditsEarned,
                                   List<TranscriptCourseDto> courses) {
        this.semesterNumber = semesterNumber;
        this.finalized = finalized;
        this.semesterGpa = semesterGpa;
        this.creditsEarned = creditsEarned;
        this.courses = courses;
    }

    public int getSemesterNumber() {
        return semesterNumber;
    }

    public boolean isFinalized() {
        return finalized;
    }

    public double getSemesterGpa() {
        return semesterGpa;
    }

    public int getCreditsEarned() {
        return creditsEarned;
    }

    public List<TranscriptCourseDto> getCourses() {
        return courses;
    }
}
