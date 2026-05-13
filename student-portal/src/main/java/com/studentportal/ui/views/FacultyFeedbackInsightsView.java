package com.studentportal.ui.views;

import com.studentportal.model.UserRole;
import com.studentportal.service.FacultyWorkflowService;
import com.studentportal.ui.SessionService;
import com.studentportal.ui.layout.MainLayout;
import com.studentportal.ui.security.AllowedRoles;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.stream.Collectors;

@Route(value = "faculty/feedback", layout = MainLayout.class)
@PageTitle("FAST Portal | Faculty Feedback Insights")
@AllowedRoles({UserRole.FACULTY})
public class FacultyFeedbackInsightsView extends VerticalLayout {

    public FacultyFeedbackInsightsView(FacultyWorkflowService facultyWorkflowService) {
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
        header.add(new H2("Anonymous student feedback"));
        header.add(new Paragraph("Responses are aggregated per assigned course-section with no student identity."));
        add(header);

        Grid<FacultyWorkflowService.FeedbackInsightRow> grid = new Grid<>(FacultyWorkflowService.FeedbackInsightRow.class, false);
        grid.addColumn(FacultyWorkflowService.FeedbackInsightRow::courseCode).setHeader("Course").setAutoWidth(true);
        grid.addColumn(FacultyWorkflowService.FeedbackInsightRow::section).setHeader("Section").setAutoWidth(true);
        grid.addColumn(FacultyWorkflowService.FeedbackInsightRow::submissions).setHeader("Responses").setAutoWidth(true);
        grid.addColumn(FacultyWorkflowService.FeedbackInsightRow::overallRating).setHeader("Overall (1-5)").setAutoWidth(true);
        grid.addColumn(row -> row.questionAverages().entrySet().stream()
                .map(entry -> entry.getKey() + ": " + String.format("%.2f", entry.getValue()))
                .collect(Collectors.joining(" | ")))
                .setHeader("Question-wise average")
                .setFlexGrow(1);
        grid.setWidthFull();
        grid.getStyle().set("max-width", "1100px");
        grid.getStyle().set("margin", "0 auto");
        grid.setAllRowsVisible(true);

        grid.setItems(facultyWorkflowService.getFeedbackInsights(SessionService.requireUserId()));
        add(grid);
    }
}
