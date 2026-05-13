package com.studentportal.model;

public enum AssessmentCategory {
    QUIZ("Quiz"),
    ASSIGNMENT("Assignment"),
    PROJECT("Project"),
    SESSIONAL1("Sessional I"),
    SESSIONAL2("Sessional II"),
    FINAL("Final Exam");

    private final String label;

    AssessmentCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
