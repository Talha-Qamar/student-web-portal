package com.studentportal.dto;

import com.studentportal.model.AssessmentCategory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class AssessmentCourseDto {

    private final Long courseId;
    private final String code;
    private final String title;
    private final double earnedAbsolute;
    private final double maxAbsolute;
    private final Map<AssessmentCategory, List<AssessmentItemDto>> breakdown;

    public AssessmentCourseDto(Long courseId,
                               String code,
                               String title,
                               double earnedAbsolute,
                               double maxAbsolute,
                               Map<AssessmentCategory, List<AssessmentItemDto>> breakdown) {
        this.courseId = courseId;
        this.code = code;
        this.title = title;
        this.earnedAbsolute = earnedAbsolute;
        this.maxAbsolute = maxAbsolute;
        this.breakdown = new EnumMap<>(breakdown);
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

    public double getEarnedAbsolute() {
        return earnedAbsolute;
    }

    public double getMaxAbsolute() {
        return maxAbsolute;
    }

    public Map<AssessmentCategory, List<AssessmentItemDto>> getBreakdown() {
        return breakdown;
    }

    public double getEarnedPercentage() {
        if (maxAbsolute <= 0) {
            return 0;
        }
        return (earnedAbsolute / maxAbsolute) * 100.0;
    }
}
