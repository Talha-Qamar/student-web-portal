package com.studentportal.ui.views;

import com.studentportal.model.UserRole;
import com.studentportal.service.FacultyWorkflowService;
import com.studentportal.ui.SessionService;
import com.studentportal.ui.layout.MainLayout;
import com.studentportal.ui.security.AllowedRoles;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "faculty/dashboard", layout = MainLayout.class)
@PageTitle("FAST Portal | Faculty Dashboard")
@AllowedRoles({UserRole.FACULTY})
public class FacultyDashboardView extends VerticalLayout {

    public FacultyDashboardView(FacultyWorkflowService facultyWorkflowService) {
        setPadding(true);
        setSpacing(true);
        setWidthFull();
        getStyle().set("align-items", "center");
        getStyle().set("text-align", "center");

        VerticalLayout header = new VerticalLayout();
        header.setPadding(false);
        header.setSpacing(false);
        header.setWidthFull();
        header.getStyle().set("max-width", "1100px");
        header.getStyle().set("margin", "0 auto");
        header.getStyle().set("text-align", "center");
        header.add(new H2("Faculty workspace"));
        header.add(new Paragraph("Manage assigned course-sections, upload attendance/grades, and review anonymous feedback."));
        add(header);

        long assignments = facultyWorkflowService.getAssignmentsForFaculty(SessionService.requireUserId()).size();
        long feedbackRows = facultyWorkflowService.getFeedbackInsights(SessionService.requireUserId()).size();

        HorizontalLayout stats = new HorizontalLayout(
                statCard("Assigned sections", String.valueOf(assignments), "Only these sections can be updated"),
                statCard("Feedback dashboards", String.valueOf(feedbackRows), "Anonymous student submissions"));
        stats.setWidthFull();
        stats.getStyle().set("max-width", "1100px");
        stats.getStyle().set("margin", "0 auto");
        stats.setJustifyContentMode(JustifyContentMode.CENTER);
        add(stats);
    }

    private Div statCard(String title, String value, String subtitle) {
        Div card = new Div();
        card.addClassName("stat-card");
        card.add(new Paragraph(title));
        card.add(new H3(value));
        card.add(new Paragraph(subtitle));
        return card;
    }
}
