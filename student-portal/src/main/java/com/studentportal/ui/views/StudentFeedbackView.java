package com.studentportal.ui.views;

import com.studentportal.model.FeedbackQuestion;
import com.studentportal.model.UserRole;
import com.studentportal.service.StudentFeedbackService;
import com.studentportal.ui.SessionService;
import com.studentportal.ui.layout.MainLayout;
import com.studentportal.ui.security.AllowedRoles;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Route(value = "student/feedback", layout = MainLayout.class)
@PageTitle("FAST Portal | Faculty Feedback")
@AllowedRoles({UserRole.STUDENT})
public class StudentFeedbackView extends VerticalLayout {

    private final StudentFeedbackService feedbackService;
    private final ComboBox<StudentFeedbackService.StudentAssignmentOption> assignmentSelect = new ComboBox<>("Enrolled course section");
    private final VerticalLayout questionsLayout = new VerticalLayout();
    private final Map<Long, RadioButtonGroup<Integer>> ratingsByQuestion = new HashMap<>();

    public StudentFeedbackView(StudentFeedbackService feedbackService) {
        this.feedbackService = feedbackService;

        setPadding(true);
        setSpacing(true);
        add(new H2("Faculty feedback"));
        add(new Paragraph("Submit anonymous feedback for your enrolled course-sections. 1 = strongly disagree, 5 = strongly agree."));

        assignmentSelect.setWidth("520px");
        assignmentSelect.setItemLabelGenerator(option -> option.courseCode() + " - " + option.courseTitle() + " (Section " + option.section() + ")");

        Button submit = new Button("Submit feedback", event -> submitFeedback());
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(assignmentSelect, questionsLayout, submit);
        reload();
    }

    private void reload() {
        Long studentId = SessionService.requireStudentId();
        List<StudentFeedbackService.StudentAssignmentOption> options = feedbackService.getEligibleAssignments(studentId);
        assignmentSelect.setItems(options);

        List<FeedbackQuestion> questions = feedbackService.getQuestions();
        questionsLayout.removeAll();
        ratingsByQuestion.clear();

        for (FeedbackQuestion question : questions) {
            RadioButtonGroup<Integer> group = new RadioButtonGroup<>();
            group.setLabel(question.getPrompt());
            group.setItems(1, 2, 3, 4, 5);
            group.setValue(4);
            group.addThemeName("helper-above-field");
            questionsLayout.add(group);
            ratingsByQuestion.put(question.getId(), group);
        }
    }

    private void submitFeedback() {
        StudentFeedbackService.StudentAssignmentOption assignment = assignmentSelect.getValue();
        if (assignment == null) {
            Notification.show("Please select an enrolled course section first.");
            return;
        }

        Map<Long, Integer> ratings = new HashMap<>();
        for (Map.Entry<Long, RadioButtonGroup<Integer>> entry : ratingsByQuestion.entrySet()) {
            ratings.put(entry.getKey(), entry.getValue().getValue());
        }

        try {
            feedbackService.submitFeedback(SessionService.requireStudentId(), assignment.assignmentId(), ratings);
            Notification.show("Feedback submitted successfully. Faculty can only view this anonymously.");
            reload();
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }
}
