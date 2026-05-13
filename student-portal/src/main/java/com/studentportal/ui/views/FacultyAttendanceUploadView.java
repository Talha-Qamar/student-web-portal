package com.studentportal.ui.views;

import com.studentportal.model.AttendanceRecord;
import com.studentportal.model.AttendanceStatus;
import com.studentportal.model.Enrollment;
import com.studentportal.model.UserRole;
import com.studentportal.service.FacultyWorkflowService;
import com.studentportal.ui.SessionService;
import com.studentportal.ui.layout.MainLayout;
import com.studentportal.ui.security.AllowedRoles;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "faculty/attendance", layout = MainLayout.class)
@PageTitle("FAST Portal | Faculty Attendance Upload")
@AllowedRoles({UserRole.FACULTY})
public class FacultyAttendanceUploadView extends VerticalLayout {

    private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final FacultyWorkflowService facultyWorkflowService;
    private final ComboBox<com.studentportal.model.CourseInstructorAssignment> assignmentSelect = new ComboBox<>("Assigned course section");
    private final DatePicker datePicker = new DatePicker("Class date");
    private final Grid<AttendanceDayRow> matrixGrid = new Grid<>(AttendanceDayRow.class, false);
    private final List<StudentRow> roster = new ArrayList<>();
    private final List<AttendanceDayRow> attendanceRows = new ArrayList<>();

    public FacultyAttendanceUploadView(FacultyWorkflowService facultyWorkflowService) {
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
        header.add(new H2("Upload attendance"));
        header.add(new Paragraph("Students are shown in ascending roll number. Rows are class dates and each cell stores P, L, or A."));
        add(header);

        assignmentSelect.setWidth("420px");
        assignmentSelect.setItemLabelGenerator(a -> a.getCourse().getCode() + " - " + a.getCourse().getTitle() + " (" + a.getSection() + ")");
        assignmentSelect.addValueChangeListener(event -> loadMatrix());

        datePicker.setValue(LocalDate.now());
        Button addRow = new Button("Add date row", event -> addAttendanceRow());
        addRow.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button save = new Button("Save attendance", event -> saveAttendance());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout controls = new HorizontalLayout(assignmentSelect, datePicker, addRow, save);
        controls.setWidthFull();
        controls.setJustifyContentMode(JustifyContentMode.CENTER);
        controls.setAlignItems(Alignment.END);
        controls.addClassName("page-controls-centered");
        add(controls);

        matrixGrid.setWidthFull();
        matrixGrid.setAllRowsVisible(true);
        matrixGrid.addClassName("page-grid-centered");

        add(new H3("Attendance matrix"), matrixGrid);
        loadAssignments();
    }

    private void loadAssignments() {
        assignmentSelect.setItems(facultyWorkflowService.getAssignmentsForFaculty(SessionService.requireUserId()));
    }

    private void loadMatrix() {
        roster.clear();
        attendanceRows.clear();
        matrixGrid.removeAllColumns();

        var assignment = assignmentSelect.getValue();
        if (assignment == null) {
            matrixGrid.setItems(List.of());
            return;
        }

        roster.addAll(facultyWorkflowService.getRoster(SessionService.requireUserId(), assignment.getId()).stream()
                .map(StudentRow::new)
                .sorted(Comparator.comparing(StudentRow::rollNumber, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList()));

        Map<LocalDate, Map<Long, AttendanceStatus>> byDate = new LinkedHashMap<>();
        for (AttendanceRecord record : facultyWorkflowService.getAttendanceRecordsForAssignment(SessionService.requireUserId(), assignment.getId())) {
            byDate.computeIfAbsent(record.getAttendanceDate(), ignored -> new LinkedHashMap<>())
                    .put(record.getStudent().getId(), record.getStatus());
        }

        if (byDate.isEmpty()) {
            attendanceRows.add(new AttendanceDayRow(LocalDate.now()));
        } else {
            byDate.forEach((date, statuses) -> attendanceRows.add(new AttendanceDayRow(date, statuses)));
        }

        buildMatrixColumns();
        matrixGrid.setItems(attendanceRows);
    }

    private void buildMatrixColumns() {
        matrixGrid.removeAllColumns();
        matrixGrid.addColumn(AttendanceDayRow::dateLabel).setHeader("Date").setAutoWidth(true);

        for (StudentRow student : roster) {
            matrixGrid.addComponentColumn(row -> buildStatusCell(row, student))
                    .setHeader(student.label())
                    .setAutoWidth(true)
                    .setFlexGrow(0);
        }
    }

    private Div buildStatusCell(AttendanceDayRow row, StudentRow student) {
        Select<AttendanceStatus> select = new Select<>();
        select.setItems(AttendanceStatus.PRESENT, AttendanceStatus.LATE, AttendanceStatus.ABSENT);
        select.setValue(row.statusFor(student.id()));
        select.setItemLabelGenerator(this::statusLabel);
        select.setWidth("100%");

        Div wrapper = new Div(select);
        wrapper.getStyle().set("min-width", "140px");
        wrapper.getStyle().set("padding", "0.35rem");
        wrapper.getStyle().set("border-radius", "8px");
        applyAttendanceColor(wrapper, select.getValue());

        select.addValueChangeListener(event -> {
            row.setStatus(student.id(), event.getValue());
            applyAttendanceColor(wrapper, event.getValue());
        });
        return wrapper;
    }

    private void applyAttendanceColor(Div wrapper, AttendanceStatus status) {
        wrapper.getStyle().remove("background-color");
        wrapper.getStyle().remove("color");
        if (AttendanceStatus.PRESENT.equals(status)) {
            wrapper.getStyle().set("background-color", "rgba(76,175,80,0.18)");
        } else if (AttendanceStatus.LATE.equals(status)) {
            wrapper.getStyle().set("background-color", "rgba(255,193,7,0.18)");
        } else if (AttendanceStatus.ABSENT.equals(status)) {
            wrapper.getStyle().set("background-color", "rgba(244,67,54,0.18)");
        }
    }

    private String statusLabel(AttendanceStatus status) {
        return switch (status) {
            case PRESENT -> "P";
            case LATE -> "L";
            case ABSENT -> "A";
        };
    }

    private void addAttendanceRow() {
        var assignment = assignmentSelect.getValue();
        if (assignment == null) {
            notify("Select an assignment first", NotificationVariant.LUMO_WARNING);
            return;
        }

        LocalDate date = datePicker.getValue();
        if (date == null) {
            notify("Pick a class date first", NotificationVariant.LUMO_ERROR);
            return;
        }

        boolean exists = attendanceRows.stream().anyMatch(row -> date.equals(row.date()));
        if (!exists) {
            attendanceRows.add(new AttendanceDayRow(date));
            attendanceRows.sort(Comparator.comparing(AttendanceDayRow::date));
            matrixGrid.setItems(attendanceRows);
        }
    }

    private void saveAttendance() {
        var assignment = assignmentSelect.getValue();
        if (assignment == null) {
            notify("Select assignment first", NotificationVariant.LUMO_WARNING);
            return;
        }

        if (roster.isEmpty()) {
            notify("No students found in this section", NotificationVariant.LUMO_ERROR);
            return;
        }

        if (attendanceRows.isEmpty()) {
            notify("Add at least one date row", NotificationVariant.LUMO_WARNING);
            return;
        }

        int written = 0;
        for (AttendanceDayRow row : attendanceRows) {
            if (row.date() == null) {
                continue;
            }
            Map<Long, AttendanceStatus> byStudent = new LinkedHashMap<>();
            for (StudentRow student : roster) {
                byStudent.put(student.id(), row.statusFor(student.id()));
            }
            written += facultyWorkflowService.uploadAttendance(SessionService.requireUserId(), assignment.getId(), row.date(), byStudent);
        }

        if (written == 0) {
            notify("No attendance rows were written", NotificationVariant.LUMO_ERROR);
            return;
        }

        notify("Attendance saved successfully (" + written + " rows)", NotificationVariant.LUMO_SUCCESS);
        loadMatrix();
    }

    private void notify(String message, NotificationVariant variant) {
        Notification notification = new Notification(message, 3000);
        notification.addThemeVariants(variant);
        notification.open();
    }

    private record StudentRow(Long id, String rollNumber, String fullName) {
        private StudentRow(Enrollment enrollment) {
            this(enrollment.getStudent().getId(), enrollment.getStudent().getRollNumber(), enrollment.getStudent().getFullName());
        }

        private String label() {
            return (rollNumber == null ? "" : rollNumber) + " • " + fullName;
        }
    }

    private static final class AttendanceDayRow {
        private final LocalDate date;
        private final Map<Long, AttendanceStatus> statuses = new LinkedHashMap<>();

        private AttendanceDayRow(LocalDate date) {
            this.date = date;
        }

        private AttendanceDayRow(LocalDate date, Map<Long, AttendanceStatus> statuses) {
            this.date = date;
            this.statuses.putAll(statuses);
        }

        private LocalDate date() {
            return date;
        }

        private String dateLabel() {
            return date == null ? "-" : DATE_LABEL.format(date);
        }

        private AttendanceStatus statusFor(Long studentId) {
            return statuses.getOrDefault(studentId, AttendanceStatus.PRESENT);
        }

        private void setStatus(Long studentId, AttendanceStatus status) {
            statuses.put(studentId, status == null ? AttendanceStatus.PRESENT : status);
        }
    }
}