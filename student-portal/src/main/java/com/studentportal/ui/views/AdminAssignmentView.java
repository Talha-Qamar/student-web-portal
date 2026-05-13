package com.studentportal.ui.views;

import com.studentportal.model.Course;
import com.studentportal.model.CourseInstructorAssignment;
import com.studentportal.model.Faculty;
import com.studentportal.model.UserRole;
import com.studentportal.service.AdminManagementService;
import com.studentportal.ui.layout.MainLayout;
import com.studentportal.ui.security.AllowedRoles;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "admin/assignments", layout = MainLayout.class)
@PageTitle("FAST Portal | Assign Instructor")
@AllowedRoles({UserRole.ADMIN})
public class AdminAssignmentView extends VerticalLayout {

    private final AdminManagementService adminManagementService;
    private final ComboBox<Course> courseSelect = new ComboBox<>("Course");
    private final ComboBox<Faculty> facultySelect = new ComboBox<>("Faculty instructor");
    private final TextField sectionField = new TextField("Section");
    private final TextField termField = new TextField("Term");
    private final Grid<CourseInstructorAssignment> grid = new Grid<>(CourseInstructorAssignment.class, false);

    public AdminAssignmentView(AdminManagementService adminManagementService) {
        this.adminManagementService = adminManagementService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        getStyle().set("align-items", "center");

        VerticalLayout header = new VerticalLayout();
        header.setPadding(false);
        header.setSpacing(false);
        header.setWidthFull();
        header.getStyle().set("max-width", "1100px");
        header.getStyle().set("margin", "0 auto");
        header.getStyle().set("text-align", "center");
        header.add(new H2("Assign course/section to instructor"));
        header.add(new Paragraph("Faculty can only upload attendance/grades for sections assigned here."));
        add(header);

        courseSelect.setItemLabelGenerator(course -> course.getCode() + " - " + course.getTitle());
        facultySelect.setItemLabelGenerator(faculty -> faculty.getFullName() + " (" + faculty.getEmail() + ")");
        sectionField.setValue("A");

        Button save = new Button("Save assignment", event -> saveAssignment());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout form = new HorizontalLayout(courseSelect, sectionField, termField, facultySelect, save);
        form.setWidthFull();
        form.setAlignItems(Alignment.END);
        form.setJustifyContentMode(JustifyContentMode.CENTER);
        form.getStyle().set("max-width", "1100px");
        form.getStyle().set("margin", "0 auto");

        configureGrid();
        grid.setWidthFull();
        grid.getStyle().set("max-width", "1100px");
        grid.getStyle().set("margin", "0 auto");
        add(form, grid);
        reload();
    }

    private void configureGrid() {
        grid.addColumn(assignment -> assignment.getCourse().getCode()).setHeader("Course").setAutoWidth(true);
        grid.addColumn(CourseInstructorAssignment::getSection).setHeader("Section").setAutoWidth(true);
        grid.addColumn(assignment -> assignment.getFaculty().getFullName()).setHeader("Instructor").setAutoWidth(true);
        grid.addColumn(CourseInstructorAssignment::getTerm).setHeader("Term").setAutoWidth(true);
        grid.setAllRowsVisible(true);
    }

    private void saveAssignment() {
        if (courseSelect.getValue() == null || facultySelect.getValue() == null) {
            Notification.show("Select both course and faculty.");
            return;
        }
        adminManagementService.assignInstructor(
                courseSelect.getValue().getId(),
                sectionField.getValue(),
                facultySelect.getValue().getId(),
                termField.getValue());
        Notification.show("Assignment saved.");
        reload();
    }

    private void reload() {
        courseSelect.setItems(adminManagementService.getCourses());
        facultySelect.setItems(adminManagementService.getFacultyUsers());
        grid.setItems(adminManagementService.getAssignments());
    }
}
