package com.studentportal.ui.views;

import com.studentportal.model.AssessmentCategory;
import com.studentportal.model.Enrollment;
import com.studentportal.model.UserRole;
import com.studentportal.service.FacultyWorkflowService;
import com.studentportal.ui.SessionService;
import com.studentportal.ui.layout.MainLayout;
import com.studentportal.ui.security.AllowedRoles;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Route(value = "faculty/grades", layout = MainLayout.class)
@PageTitle("FAST Portal | Faculty Grade Upload")
@AllowedRoles({UserRole.FACULTY})
public class FacultyGradeUploadView extends VerticalLayout {

    private final FacultyWorkflowService facultyWorkflowService;
    private final ComboBox<com.studentportal.model.CourseInstructorAssignment> assignmentSelect = new ComboBox<>("Assigned course section");
    private final Grid<Enrollment> matrixGrid = new Grid<>(Enrollment.class, false);
    private final Select<AssessmentCategory> categorySelect = new Select<>();
    private final NumberField totalMarksField = new NumberField("Total marks");
    private final NumberField weightField = new NumberField("Absolute weight");
    private final NumberField defaultMarkField = new NumberField("Default mark");

    private final List<Enrollment> roster = new ArrayList<>();
    private final List<AssessmentColumnState> assessmentColumns = new ArrayList<>();

    public FacultyGradeUploadView(FacultyWorkflowService facultyWorkflowService) {
        this.facultyWorkflowService = facultyWorkflowService;
        setPadding(true);
        setSpacing(true);
        setWidthFull();
        addClassName("page-shell");

        VerticalLayout header = new VerticalLayout();
        header.setPadding(false);
        header.setSpacing(false);
        header.setWidthFull();
        header.getStyle().set("text-align", "center");
        header.add(new H2("Upload marks"));
        header.add(new Paragraph("Students are shown in ascending roll number. Add assessment columns for quizzes, sessionals, assignments, or project work."));
        add(header);

        assignmentSelect.setWidth("420px");
        assignmentSelect.setItemLabelGenerator(a -> a.getCourse().getCode() + " - " + a.getCourse().getTitle() + " (" + a.getSection() + ")");
        assignmentSelect.addValueChangeListener(event -> loadMatrix());

        categorySelect.setLabel("Assessment section");
        categorySelect.setItems(AssessmentCategory.values());
        categorySelect.setValue(AssessmentCategory.QUIZ);

        totalMarksField.setValue(10.0);
        weightField.setValue(2.0);
        defaultMarkField.setValue(0.0);

        Button addColumn = new Button("Add assessment column", event -> addAssessmentColumn());
        addColumn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button save = new Button("Save marks", event -> saveMarks());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout topRow = new HorizontalLayout(assignmentSelect);
        topRow.setWidthFull();
        topRow.setJustifyContentMode(JustifyContentMode.CENTER);
        add(topRow);

        HorizontalLayout controls = new HorizontalLayout(categorySelect, totalMarksField, weightField, defaultMarkField, addColumn, save);
        controls.setWidthFull();
        controls.setJustifyContentMode(JustifyContentMode.CENTER);
        controls.setAlignItems(Alignment.END);
        controls.addClassName("page-controls-centered");
        add(controls);

        matrixGrid.setWidthFull();
        matrixGrid.setAllRowsVisible(true);
        matrixGrid.addClassName("page-grid-centered");
        add(new H3("Marks matrix"), matrixGrid);

        loadAssignments();
    }

    private void loadAssignments() {
        assignmentSelect.setItems(facultyWorkflowService.getAssignmentsForFaculty(SessionService.requireUserId()));
    }

    private void loadMatrix() {
        roster.clear();
        assessmentColumns.clear();
        matrixGrid.removeAllColumns();

        var assignment = assignmentSelect.getValue();
        if (assignment == null) {
            matrixGrid.setItems(List.of());
            return;
        }

        roster.addAll(facultyWorkflowService.getRoster(SessionService.requireUserId(), assignment.getId()));
        roster.sort(Comparator.comparing(
            enrollment -> enrollment.getStudent().getRollNumber(),
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        // Load existing assessments from database
        loadExistingAssessments(assignment.getCourse().getId());
        
        buildMatrixColumns();
        matrixGrid.setItems(roster);
    }

    private void loadExistingAssessments(Long courseId) {
        // Load distinct assessments from database
        var existingAssessments = facultyWorkflowService.getExistingAssessments(courseId);
        
        for (var assessment : existingAssessments) {
            // Create a map with existing marks for all students
            Map<Long, Double> values = new LinkedHashMap<>();
            for (Enrollment enrollment : roster) {
                Double mark = 0.0;
                // Try to find existing mark for this student/assessment
                // (will be populated from database when getStudentAssessmentMarks is called per student)
                values.put(enrollment.getStudent().getId(), mark);
            }
            
            assessmentColumns.add(new AssessmentColumnState(
                    assessment.category(),
                    assessment.title(),
                    assessment.totalMarks(),
                    assessment.absoluteWeight(),
                    values
            ));
        }
        
        // Load actual marks from database for each student
        if (!roster.isEmpty() && !existingAssessments.isEmpty()) {
            var firstAssignment = assignmentSelect.getValue();
            if (firstAssignment != null) {
                Long courseId2 = firstAssignment.getCourse().getId();
                for (Enrollment enrollment : roster) {
                    var marks = facultyWorkflowService.getStudentAssessmentMarks(enrollment.getStudent().getId(), courseId2);
                    for (int i = 0; i < assessmentColumns.size(); i++) {
                        var column = assessmentColumns.get(i);
                        String key = column.category() + "|" + column.title();
                        if (marks.containsKey(key)) {
                            column.values().put(enrollment.getStudent().getId(), marks.get(key));
                        }
                    }
                }
            }
        }
    }

    private void buildMatrixColumns() {
        matrixGrid.removeAllColumns();
        matrixGrid.addColumn(enrollment -> enrollment.getStudent().getRollNumber()).setHeader("Roll #").setAutoWidth(true);
        matrixGrid.addColumn(enrollment -> enrollment.getStudent().getFullName()).setHeader("Student").setFlexGrow(1);

        for (AssessmentColumnState column : assessmentColumns) {
            matrixGrid.addComponentColumn(enrollment -> buildMarkCell(column, enrollment))
                    .setHeader(column.title())
                    .setAutoWidth(true)
                    .setFlexGrow(0);
        }
    }

    private Div buildMarkCell(AssessmentColumnState column, Enrollment enrollment) {
        NumberField field = new NumberField();
        field.setWidth("120px");
        field.setMin(0);
        field.setMax(column.totalMarks());
        field.setStep(0.25);
        field.setValue(column.values().getOrDefault(enrollment.getStudent().getId(), defaultMarkField.getValue()));

        Div wrapper = new Div(field);
        wrapper.getStyle().set("padding", "0.25rem");
        wrapper.getStyle().set("min-width", "132px");
        wrapper.getStyle().set("border-radius", "8px");
        wrapper.getStyle().set("background-color", "rgba(63,81,181,0.06)");

        field.addValueChangeListener(event -> column.values().put(enrollment.getStudent().getId(), event.getValue()));
        return wrapper;
    }

    private void addAssessmentColumn() {
        if (assignmentSelect.getValue() == null) {
            notify("Select assignment first", NotificationVariant.LUMO_WARNING);
            return;
        }

        if (categorySelect.getValue() == null) {
            notify("Select an assessment section", NotificationVariant.LUMO_ERROR);
            return;
        }

        String newLabel = categorySelect.getValue().getLabel() + " " + (assessmentColumns.size() + 1);
        
        // Check if an assessment with this category and label already exists
        for (AssessmentColumnState existing : assessmentColumns) {
            if (existing.category().equals(categorySelect.getValue()) && existing.title().equals(newLabel)) {
                notify("This assessment column already exists", NotificationVariant.LUMO_WARNING);
                return;
            }
        }

        assessmentColumns.add(new AssessmentColumnState(
                categorySelect.getValue(),
                newLabel,
                totalMarksField.getValue() == null ? 0.0 : totalMarksField.getValue(),
                weightField.getValue() == null ? 0.0 : weightField.getValue(),
            roster.stream().collect(java.util.stream.Collectors.toMap(
                enrollment -> enrollment.getStudent().getId(),
                enrollment -> defaultMarkField.getValue() == null ? 0.0 : defaultMarkField.getValue(),
                (left, right) -> right,
                LinkedHashMap::new))));
        buildMatrixColumns();
        matrixGrid.setItems(roster);
        notify("Assessment column added", NotificationVariant.LUMO_SUCCESS);
    }

    private void saveMarks() {
        var assignment = assignmentSelect.getValue();
        if (assignment == null) {
            notify("Select assignment first", NotificationVariant.LUMO_WARNING);
            return;
        }

        if (assessmentColumns.isEmpty()) {
            notify("Add at least one assessment column", NotificationVariant.LUMO_WARNING);
            return;
        }

        for (AssessmentColumnState column : assessmentColumns) {
            for (Enrollment enrollment : roster) {
                Double score = column.values().get(enrollment.getStudent().getId());
                facultyWorkflowService.saveAssessmentScore(
                        SessionService.requireUserId(),
                        assignment.getId(),
                        enrollment.getStudent().getId(),
                        column.category(),
                        column.title(),
                        score == null ? 0.0 : score,
                        column.totalMarks(),
                        column.absoluteWeight());
            }
        }

        notify("Marks saved successfully", NotificationVariant.LUMO_SUCCESS);
        loadMatrix();
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = new Notification(message, 3000);
        notification.addThemeVariants(variant);
        notification.open();
    }

    private record AssessmentColumnState(AssessmentCategory category,
                                          String title,
                                          Double totalMarks,
                                          Double absoluteWeight,
                                          Map<Long, Double> values) {
    }
}