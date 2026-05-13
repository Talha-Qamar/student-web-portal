package com.studentportal.ui.views;

import com.studentportal.model.UserRole;
import com.studentportal.service.AdminManagementService;
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

@Route(value = "admin/dashboard", layout = MainLayout.class)
@PageTitle("FAST Portal | Admin Dashboard")
@AllowedRoles({UserRole.ADMIN})
public class AdminDashboardView extends VerticalLayout {

    public AdminDashboardView(AdminManagementService adminManagementService) {
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
        header.add(new H2("Admin workspace"));
        header.add(new Paragraph("Assign instructors by course-section and maintain faculty/student records."));
        add(header);

        HorizontalLayout stats = new HorizontalLayout(
                statCard("Students", String.valueOf(adminManagementService.getStudents().size()), "Editable profile records"),
                statCard("Faculty", String.valueOf(adminManagementService.getFacultyUsers().size()), "Instructor directory"),
                statCard("Assignments", String.valueOf(adminManagementService.getAssignments().size()), "Course-section mappings")
        );
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
