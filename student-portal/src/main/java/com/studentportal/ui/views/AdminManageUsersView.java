package com.studentportal.ui.views;

import com.studentportal.model.Faculty;
import com.studentportal.model.Student;
import com.studentportal.model.UserRole;
import com.studentportal.service.AdminManagementService;
import com.studentportal.ui.layout.MainLayout;
import com.studentportal.ui.security.AllowedRoles;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "admin/users", layout = MainLayout.class)
@PageTitle("FAST Portal | Edit Student and Faculty")
@AllowedRoles({UserRole.ADMIN})
public class AdminManageUsersView extends VerticalLayout {

    private final AdminManagementService adminManagementService;

    private final TextField studentRollSearch = new TextField("Roll number");
    private final TextField studentRollNumber = new TextField("Roll number");
    private final TextField studentName = new TextField("Full name");
    private final EmailField studentEmail = new EmailField("Email");
    private final TextField studentMajor = new TextField("Major");
    private final IntegerField studentSemester = new IntegerField("Current semester");
    private final Div studentDetailsPanel = new Div();
    private Student selectedStudent;

    private final ComboBox<Faculty> facultySelect = new ComboBox<>("Select faculty");
    private final TextField facultyName = new TextField("Full name");
    private final EmailField facultyEmail = new EmailField("Email");
    private final TextField facultyDepartment = new TextField("Department");
    private final TextField facultyDesignation = new TextField("Designation");
    private final Div facultyDetailsPanel = new Div();
    private Faculty selectedFaculty;

    public AdminManageUsersView(AdminManagementService adminManagementService) {
        this.adminManagementService = adminManagementService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        addClassName("page-shell");

        VerticalLayout header = new VerticalLayout();
        header.setPadding(false);
        header.setSpacing(false);
        header.setWidthFull();
        header.getStyle().set("text-align", "center");
        header.add(new H2("Manage user profiles"));
        header.add(new Paragraph("Search students by roll number and edit student/faculty records in place."));
        add(header);

        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        tabs.add("Students", buildStudentTab());
        tabs.add("Faculty", buildFacultyTab());
        add(tabs);

        reload();
    }

    private VerticalLayout buildStudentTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setWidthFull();
        layout.addClassName("page-shell-compact");

        VerticalLayout searchBlock = new VerticalLayout();
        searchBlock.setPadding(false);
        searchBlock.setSpacing(true);
        searchBlock.setWidthFull();
        searchBlock.getStyle().set("align-items", "center");
        searchBlock.add(new H3("Search student by roll number"));

        studentRollSearch.setPlaceholder("23I-0013");
        studentRollSearch.setWidth("280px");
        Button searchButton = new Button("Search", event -> searchStudent());
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout searchRow = new HorizontalLayout(studentRollSearch, searchButton);
        searchRow.setAlignItems(Alignment.END);
        searchRow.setJustifyContentMode(JustifyContentMode.CENTER);
        searchRow.setWidthFull();
        searchBlock.add(searchRow);

        studentDetailsPanel.setVisible(false);
        studentDetailsPanel.getStyle().set("width", "100%");
        studentDetailsPanel.getStyle().set("max-width", "900px");
        studentDetailsPanel.getStyle().set("margin", "0 auto");
        studentDetailsPanel.getStyle().set("padding", "1.5rem");
        studentDetailsPanel.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        studentDetailsPanel.getStyle().set("border-radius", "12px");
        studentDetailsPanel.getStyle().set("background", "var(--lumo-shade-5pct)");

        VerticalLayout form = new VerticalLayout();
        form.setPadding(false);
        form.setSpacing(true);
        form.add(new H3("Student details"));

        studentRollNumber.setWidthFull();
        studentName.setWidthFull();
        studentEmail.setWidthFull();
        studentMajor.setWidthFull();
        studentSemester.setWidthFull();

        Button save = new Button("Save changes", event -> saveStudent());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button clear = new Button("Clear", event -> clearStudentSelection());
        clear.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout actions = new HorizontalLayout(save, clear);
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.CENTER);

        form.add(studentRollNumber, studentName, studentEmail, studentMajor, studentSemester, actions);
        studentDetailsPanel.add(form);

        layout.add(searchBlock, studentDetailsPanel);
        return layout;
    }

    private VerticalLayout buildFacultyTab() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setWidthFull();
        layout.addClassName("page-shell-compact");

        facultySelect.setItems(adminManagementService.getFacultyUsers());
        facultySelect.setItemLabelGenerator(faculty -> faculty.getFullName() + " (" + faculty.getEmail() + ")");
        facultySelect.setWidth("420px");
        facultySelect.addValueChangeListener(event -> {
            selectedFaculty = event.getValue();
            if (selectedFaculty != null) {
                loadFacultyDetails(selectedFaculty);
            } else {
                clearFacultySelection();
            }
        });

        HorizontalLayout selectRow = new HorizontalLayout(facultySelect);
        selectRow.setWidthFull();
        selectRow.setJustifyContentMode(JustifyContentMode.CENTER);

        facultyDetailsPanel.setVisible(false);
        facultyDetailsPanel.getStyle().set("width", "100%");
        facultyDetailsPanel.getStyle().set("max-width", "900px");
        facultyDetailsPanel.getStyle().set("margin", "0 auto");
        facultyDetailsPanel.getStyle().set("padding", "1.5rem");
        facultyDetailsPanel.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        facultyDetailsPanel.getStyle().set("border-radius", "12px");
        facultyDetailsPanel.getStyle().set("background", "var(--lumo-shade-5pct)");

        VerticalLayout form = new VerticalLayout();
        form.setPadding(false);
        form.setSpacing(true);
        form.add(new H3("Faculty details"));

        facultyName.setWidthFull();
        facultyEmail.setWidthFull();
        facultyDepartment.setWidthFull();
        facultyDesignation.setWidthFull();

        Button save = new Button("Save changes", event -> saveFaculty());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button clear = new Button("Clear", event -> clearFacultySelection());
        clear.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout actions = new HorizontalLayout(save, clear);
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.CENTER);

        form.add(facultyName, facultyEmail, facultyDepartment, facultyDesignation, actions);
        facultyDetailsPanel.add(form);

        layout.add(selectRow, facultyDetailsPanel);
        return layout;
    }

    private void searchStudent() {
        String rollNumber = studentRollSearch.getValue() == null ? "" : studentRollSearch.getValue().trim();
        if (rollNumber.isBlank()) {
            notify("Enter a roll number first", NotificationVariant.LUMO_WARNING);
            return;
        }

        adminManagementService.findStudentByRollNumber(rollNumber).ifPresentOrElse(student -> {
            selectedStudent = student;
            loadStudentDetails(student);
        }, () -> {
            selectedStudent = null;
            studentDetailsPanel.setVisible(false);
            notify("Student not found for roll number " + rollNumber, NotificationVariant.LUMO_ERROR);
        });
    }

    private void loadStudentDetails(Student student) {
        studentRollNumber.setValue(defaultText(student.getRollNumber()));
        studentName.setValue(defaultText(student.getFullName()));
        studentEmail.setValue(defaultText(student.getEmail()));
        studentMajor.setValue(defaultText(student.getMajor()));
        studentSemester.setValue(student.getCurrentSemester());
        studentDetailsPanel.setVisible(true);
    }

    private void loadFacultyDetails(Faculty faculty) {
        facultyName.setValue(defaultText(faculty.getFullName()));
        facultyEmail.setValue(defaultText(faculty.getEmail()));
        facultyDepartment.setValue(defaultText(faculty.getDepartment()));
        facultyDesignation.setValue(defaultText(faculty.getDesignation()));
        facultyDetailsPanel.setVisible(true);
    }

    private void saveStudent() {
        if (selectedStudent == null) {
            notify("Select a student first", NotificationVariant.LUMO_WARNING);
            return;
        }

        if (studentRollNumber.getValue().isBlank() || studentName.getValue().isBlank() || studentEmail.getValue().isBlank()) {
            notify("Roll number, name, and email are required", NotificationVariant.LUMO_ERROR);
            return;
        }

        if (studentSemester.getValue() == null || studentSemester.getValue() < 1 || studentSemester.getValue() > 8) {
            notify("Semester must be between 1 and 8", NotificationVariant.LUMO_ERROR);
            return;
        }

        adminManagementService.updateStudent(
                selectedStudent.getId(),
                studentName.getValue().trim(),
                studentRollNumber.getValue().trim().toUpperCase(),
                studentEmail.getValue().trim(),
                studentMajor.getValue().trim(),
                studentSemester.getValue());
        notify("Student updated successfully", NotificationVariant.LUMO_SUCCESS);
        reload();
    }

    private void saveFaculty() {
        if (selectedFaculty == null) {
            notify("Select a faculty member first", NotificationVariant.LUMO_WARNING);
            return;
        }

        if (facultyName.getValue().isBlank() || facultyEmail.getValue().isBlank()) {
            notify("Name and email are required", NotificationVariant.LUMO_ERROR);
            return;
        }

        adminManagementService.updateFaculty(
                selectedFaculty.getId(),
                facultyName.getValue().trim(),
                facultyEmail.getValue().trim(),
                facultyDepartment.getValue().trim(),
                facultyDesignation.getValue().trim());
        notify("Faculty updated successfully", NotificationVariant.LUMO_SUCCESS);
        reload();
    }

    private void clearStudentSelection() {
        selectedStudent = null;
        studentRollSearch.clear();
        studentRollNumber.clear();
        studentName.clear();
        studentEmail.clear();
        studentMajor.clear();
        studentSemester.clear();
        studentDetailsPanel.setVisible(false);
    }

    private void clearFacultySelection() {
        selectedFaculty = null;
        facultySelect.clear();
        facultyName.clear();
        facultyEmail.clear();
        facultyDepartment.clear();
        facultyDesignation.clear();
        facultyDetailsPanel.setVisible(false);
    }

    private void reload() {
        facultySelect.setItems(adminManagementService.getFacultyUsers());
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = new Notification(message, 3000);
        notification.addThemeVariants(variant);
        notification.open();
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }
}