package com.studentportal.ui.views;

import com.studentportal.dto.AttendanceCourseDto;
import com.studentportal.dto.AttendanceEntryDto;
import com.studentportal.model.AttendanceStatus;
import com.studentportal.model.UserRole;
import com.studentportal.service.AttendanceService;
import com.studentportal.ui.SessionService;
import com.studentportal.ui.layout.MainLayout;
import com.studentportal.ui.security.AllowedRoles;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Route(value = "student/attendance", layout = MainLayout.class)
@PageTitle("FAST Portal | Attendance")
@AllowedRoles({UserRole.STUDENT})
public class AttendanceView extends VerticalLayout {

    private static final DateTimeFormatter LEDGER_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM");

    private final AttendanceService attendanceService;
    private final Select<Integer> semesterSelect = new Select<>();
    private final Div courseButtonsContainer = new Div();
    private final Div detailsPanel = new Div();
    private final Div emptyState = new Div();
    private final Div semesterSelectorContainer = new Div();

    private Long studentId;

    public AttendanceView(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
        setSpacing(false);
        setPadding(false);
        addClassName("attendance-view");

        // Header section
        VerticalLayout headerSection = new VerticalLayout();
        headerSection.setSpacing(false);
        headerSection.setPadding(true);
        headerSection.setWidthFull();
        headerSection.addClassName("page-header-centered");
        headerSection.getStyle().set("text-align", "center");
        headerSection.add(new H2("Attendance Tracker"));
        add(headerSection);

        // Semester selector - centered
        semesterSelectorContainer.addClassName("semester-selector-container");
        semesterSelectorContainer.addClassName("page-shell-compact");
        semesterSelectorContainer.getStyle().set("display", "flex");
        semesterSelectorContainer.getStyle().set("justify-content", "center");
        semesterSelectorContainer.getStyle().set("align-items", "center");
        semesterSelectorContainer.getStyle().set("padding", "2rem");
        semesterSelectorContainer.getStyle().set("background", "var(--lumo-shade-5pct)");
        semesterSelectorContainer.getStyle().set("width", "100%");

        configureSemesterSelect();
        semesterSelectorContainer.add(semesterSelect);
        add(semesterSelectorContainer);

        // Courses buttons container
        courseButtonsContainer.addClassName("course-buttons");
        courseButtonsContainer.addClassName("page-shell-compact");
        courseButtonsContainer.getStyle().set("display", "flex");
        courseButtonsContainer.getStyle().set("gap", "0.5rem");
        courseButtonsContainer.getStyle().set("flex-wrap", "wrap");
        courseButtonsContainer.getStyle().set("justify-content", "center");
        courseButtonsContainer.getStyle().set("padding", "1.5rem");
        courseButtonsContainer.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-20pct)");
        courseButtonsContainer.getStyle().set("width", "100%");
        courseButtonsContainer.getStyle().set("max-width", "960px");
        courseButtonsContainer.getStyle().set("margin", "0 auto");
        courseButtonsContainer.setVisible(false);
        add(courseButtonsContainer);

        // Details panel
        detailsPanel.addClassName("attendance-details");
        detailsPanel.addClassName("page-shell-compact");
        detailsPanel.getStyle().set("padding", "2rem");
        detailsPanel.getStyle().set("width", "100%");
        detailsPanel.getStyle().set("max-width", "960px");
        detailsPanel.getStyle().set("margin", "0 auto");
        detailsPanel.setVisible(false);
        add(detailsPanel);

        // Empty state
        emptyState.addClassName("empty-state");
        emptyState.addClassName("section-centered");
        emptyState.getStyle().set("padding", "2rem");
        emptyState.getStyle().set("text-align", "center");
        emptyState.getStyle().set("width", "100%");
        emptyState.add(new Paragraph("Select a semester to view your attendance"));
        add(emptyState);

        refresh();
    }

    private void configureSemesterSelect() {
        semesterSelect.setLabel("Select Semester");
        semesterSelect.setPlaceholder("Choose semester");
        semesterSelect.setWidth("250px");
        semesterSelect.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                loadSemester(event.getValue());
            }
        });
    }

    private void refresh() {
        try {
            studentId = SessionService.requireStudentId();
            List<Integer> semesters = attendanceService.getTrackedSemesters(studentId);
            semesterSelect.setItems(semesters);
        } catch (IllegalStateException ex) {
            Notification.show("You need to sign in again to view attendance");
        }
    }

    private void loadSemester(Integer semesterNumber) {
        if (studentId == null) {
            return;
        }
        List<AttendanceCourseDto> courses = attendanceService.getAttendanceForSemester(studentId, semesterNumber);
        renderCourseButtons(courses);
        courseButtonsContainer.setVisible(!courses.isEmpty());
        emptyState.setVisible(courses.isEmpty());
        if (!courses.isEmpty()) {
            detailsPanel.removeAll();
            detailsPanel.setVisible(false);
        }
    }

    private void renderCourseButtons(List<AttendanceCourseDto> courses) {
        courseButtonsContainer.removeAll();
        for (AttendanceCourseDto course : courses) {
            Button courseBtn = new Button(course.getCode(), event -> displayCourseDetails(course));
            courseBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            // Add visual indicator for attendance risk
            double percentage = course.getAttendancePercentage();
            if (percentage < 60) {
                courseBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            } else if (percentage < 80) {
                // Add custom styling for warning state
                courseBtn.getStyle().set("background-color", "var(--lumo-warning-color, #FFF3CD)");
                courseBtn.getStyle().set("color", "var(--lumo-body-text-color)");
            }

            courseButtonsContainer.add(courseBtn);
        }
    }

    private void displayCourseDetails(AttendanceCourseDto course) {
        detailsPanel.removeAll();

        // Course header with progress
        HorizontalLayout courseHeader = new HorizontalLayout();
        courseHeader.setWidthFull();
        courseHeader.setAlignItems(Alignment.CENTER);

        VerticalLayout courseInfo = new VerticalLayout();
        courseInfo.setSpacing(false);
        courseInfo.setPadding(false);
        H3 courseTitle = new H3(course.getCode() + " - " + course.getTitle());
        courseInfo.add(courseTitle);

        Component progressComponent = buildProgressBar(course);
        courseInfo.add(progressComponent);

        Paragraph sessionInfo = new Paragraph("Sessions: " + course.getPresentSessions() + " / " + course.getTotalSessions());
        sessionInfo.getStyle().set("margin", "0.5rem 0 0 0");
        sessionInfo.getStyle().set("color", "var(--lumo-secondary-text-color)");
        sessionInfo.getStyle().set("font-size", "0.9rem");
        courseInfo.add(sessionInfo);

        courseHeader.add(courseInfo);

        Button backBtn = new Button("Back to Semesters", VaadinIcon.ARROW_LEFT.create(), event -> {
            detailsPanel.setVisible(false);
        });
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        courseHeader.add(backBtn);
        detailsPanel.add(courseHeader);

        // Attendance ledger
        createAttendanceLedger(course, detailsPanel);

        detailsPanel.setVisible(true);
    }

    private void createAttendanceLedger(AttendanceCourseDto course, Div container) {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(true);
        section.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        section.getStyle().set("border-radius", "4px");
        section.getStyle().set("background", "var(--lumo-shade-5pct)");
        section.getStyle().set("width", "100%");
        section.getStyle().set("max-width", "920px");
        section.getStyle().set("margin", "0 auto");

        H3 sectionTitle = new H3("Attendance Ledger");
        section.add(sectionTitle);

        Grid<AttendanceEntryDto> ledger = new Grid<>(AttendanceEntryDto.class, false);
        ledger.addColumn(entry -> entry.getDate() != null ? LEDGER_FORMAT.format(entry.getDate()) : "-")
                .setHeader("Date")
                .setAutoWidth(true);
        ledger.addColumn(AttendanceEntryDto::getDay)
                .setHeader("Day")
                .setAutoWidth(true);
        ledger.addComponentColumn(entry -> buildStatusChip(entry.getStatus()))
                .setHeader("Status")
                .setAutoWidth(true);

        List<AttendanceEntryDto> entries = course.getEntries() == null ? Collections.emptyList() : course.getEntries();
        ledger.setItems(entries);
        ledger.setAllRowsVisible(true);
        ledger.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
        ledger.setWidthFull();

        section.add(ledger);
        container.add(section);
    }

    private Component buildProgressBar(AttendanceCourseDto dto) {
        ProgressBar bar = new ProgressBar(0, 100, dto.getAttendancePercentage());
        bar.setWidth("200px");
        if (dto.getAttendancePercentage() < 60) {
            bar.getElement().getThemeList().add("error");
        } else if (dto.getAttendancePercentage() < 80) {
            bar.getElement().getThemeList().add("warning");
        }

        Span caption = new Span(formatPercentage(dto.getAttendancePercentage()));
        caption.getStyle().set("font-weight", "600");
        caption.getStyle().set("margin-left", "1rem");

        HorizontalLayout wrapper = new HorizontalLayout(bar, caption);
        wrapper.setAlignItems(Alignment.CENTER);
        return wrapper;
    }

    private Component buildStatusChip(AttendanceStatus status) {
        Span chip = new Span(status != null ? status.name() : "-");
        chip.addClassName("status-chip");
        if (AttendanceStatus.ABSENT.equals(status)) {
            chip.addClassName("danger");
        }
        return chip;
    }

    private String formatPercentage(double value) {
        return String.format("%.1f%%", value);
    }
}

