package com.studentportal.dto;

import com.studentportal.model.AssessmentCategory;

public class AssessmentItemDto {

    private final AssessmentCategory category;
    private final String title;
    private final double obtainedMarks;
    private final double totalMarks;
    private final double absoluteWeight;
    private final double absoluteEarned;

    public AssessmentItemDto(AssessmentCategory category,
                             String title,
                             double obtainedMarks,
                             double totalMarks,
                             double absoluteWeight,
                             double absoluteEarned) {
        this.category = category;
        this.title = title;
        this.obtainedMarks = obtainedMarks;
        this.totalMarks = totalMarks;
        this.absoluteWeight = absoluteWeight;
        this.absoluteEarned = absoluteEarned;
    }

    public AssessmentCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public double getObtainedMarks() {
        return obtainedMarks;
    }

    public double getTotalMarks() {
        return totalMarks;
    }

    public double getAbsoluteWeight() {
        return absoluteWeight;
    }

    public double getAbsoluteEarned() {
        return absoluteEarned;
    }
}
