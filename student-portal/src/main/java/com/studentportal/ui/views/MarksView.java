package com.studentportal.ui.views;

import com.studentportal.dto.AssessmentCourseDto;
import com.studentportal.dto.AssessmentItemDto;
import com.studentportal.model.AssessmentCategory;
import com.studentportal.model.UserRole;
import com.studentportal.service.AssessmentService;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Route(value = "student/marks", layout = MainLayout.class)
@PageTitle("FAST Portal | Marks")
@AllowedRoles({UserRole.STUDENT})
public class MarksView extends VerticalLayout {

    private final AssessmentService assessmentService;
    private final Select<Integer> semesterSelect = new Select<>();
    private final Div courseButtonsContainer = new Div();
    private final Div detailsPanel = new Div();
    private final Div emptyState = new Div();
    private final Div semesterSelectorContainer = new Div();

    private Long studentId;

    public MarksView(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
        setSpacing(false);
        setPadding(false);
        addClassName("marks-view");

        // Header section
        VerticalLayout headerSection = new VerticalLayout();
        headerSection.setSpacing(false);
        headerSection.setPadding(true);
        headerSection.setWidthFull();
        headerSection.addClassName("page-header-centered");
        headerSection.getStyle().set("text-align", "center");
        headerSection.add(new H2("Academic Marks"));
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
        detailsPanel.addClassName("marks-details");
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
        Paragraph emptyMsg = new Paragraph("Select a semester to view your marks");
        emptyState.add(emptyMsg);
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
            List<Integer> semesters = assessmentService.getTrackedSemesters(studentId);
            semesterSelect.setItems(semesters);
        } catch (IllegalStateException ex) {
            Notification.show("Session expired. Please sign in again to view marks.");
        }
    }

    private void loadSemester(Integer semesterNumber) {
        List<AssessmentCourseDto> courses = assessmentService.getAssessmentsForSemester(studentId, semesterNumber);
        renderCourseButtons(courses);
        courseButtonsContainer.setVisible(!courses.isEmpty());
        emptyState.setVisible(courses.isEmpty());
        if (!courses.isEmpty()) {
            detailsPanel.removeAll();
            detailsPanel.setVisible(false);
        }
    }

    private void renderCourseButtons(List<AssessmentCourseDto> courses) {
        courseButtonsContainer.removeAll();
        for (AssessmentCourseDto course : courses) {
            Button courseBtn = new Button(course.getCode(), event -> displayCourseDetails(course));
            courseBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            // Add visual indicator for performance
            double percentage = course.getEarnedPercentage();
            if (percentage < 60) {
                courseBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            } else if (percentage < 85) {
                // Add custom styling for warning state
                courseBtn.getStyle().set("background-color", "var(--lumo-warning-color, #FFF3CD)");
                courseBtn.getStyle().set("color", "var(--lumo-body-text-color)");
            }

            courseButtonsContainer.add(courseBtn);
        }
    }

    private void displayCourseDetails(AssessmentCourseDto course) {
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

        Component progressComponent = buildProgressBar(course.getEarnedPercentage());
        courseInfo.add(progressComponent);

        courseHeader.add(courseInfo);

        Button backBtn = new Button("Back to Semesters", VaadinIcon.ARROW_LEFT.create(), event -> {
            detailsPanel.setVisible(false);
        });
        backBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        courseHeader.add(backBtn);
        detailsPanel.add(courseHeader);

        // Group assessments by category
        Map<AssessmentCategory, List<AssessmentItemDto>> breakdown = course.getBreakdown();
        if (breakdown != null && !breakdown.isEmpty()) {
            // Create a section for each assessment category
            for (AssessmentCategory category : AssessmentCategory.values()) {
                List<AssessmentItemDto> items = breakdown.get(category);
                if (items != null && !items.isEmpty()) {
                    createCategorySection(category, items, detailsPanel);
                }
            }
        }

        detailsPanel.setVisible(true);
    }

    private void createCategorySection(AssessmentCategory category, List<AssessmentItemDto> items, Div container) {
        List<AssessmentItemDto> uniqueRows = deduplicateItems(items);

        VerticalLayout section = new VerticalLayout();
        section.setSpacing(true);
        section.setPadding(true);
        section.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        section.getStyle().set("border-radius", "4px");
        section.getStyle().set("background", "var(--lumo-shade-5pct)");
        section.getStyle().set("width", "100%");
        section.getStyle().set("max-width", "920px");
        section.getStyle().set("margin", "0 auto 1rem auto");

        H3 categoryTitle = new H3(category.getLabel());
        section.add(categoryTitle);

        Grid<AssessmentItemDto> grid = new Grid<>(AssessmentItemDto.class, false);
        grid.addColumn(AssessmentItemDto::getTitle)
                .setHeader("Assessment")
                .setFlexGrow(1);
        grid.addColumn(item -> formatMarks(item.getObtainedMarks(), item.getTotalMarks()))
                .setHeader("Marks")
                .setAutoWidth(true);
        grid.addColumn(item -> formatPercentage(calculatePercentage(item.getObtainedMarks(), item.getTotalMarks())))
                .setHeader("Percentage")
                .setAutoWidth(true);
        grid.addColumn(item -> formatAbsolute(item.getAbsoluteEarned(), item.getAbsoluteWeight()))
                .setHeader("Absolute Score")
                .setAutoWidth(true);

        grid.setItems(uniqueRows);
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
        grid.setWidthFull();

        section.add(grid);
        container.add(section);
    }

    private Component buildProgressBar(double percentage) {
        ProgressBar bar = new ProgressBar(0, 100, percentage);
        bar.setWidth("200px");
        if (percentage < 60) {
            bar.getElement().getThemeList().add("error");
        } else if (percentage < 85) {
            bar.getElement().getThemeList().add("warning");
        }

        Span caption = new Span(formatPercentage(percentage));
        caption.getStyle().set("font-weight", "600");
        caption.getStyle().set("margin-left", "1rem");

        HorizontalLayout wrapper = new HorizontalLayout(bar, caption);
        wrapper.setAlignItems(Alignment.CENTER);
        return wrapper;
    }

    private String formatAbsolute(double earned, double max) {
        if (max <= 0) {
            return String.format("%.1f", earned);
        }
        return String.format("%.1f / %.0f", earned, max);
    }

    private String formatMarks(double obtained, double total) {
        if (total <= 0) {
            return String.format("%.1f", obtained);
        }
        return String.format("%.1f / %.1f", obtained, total);
    }

    private String formatPercentage(double value) {
        return String.format("%.1f%%", value);
    }

    private double calculatePercentage(double obtained, double total) {
        if (total <= 0) {
            return 0;
        }
        return (obtained / total) * 100;
    }

    private List<AssessmentItemDto> deduplicateItems(List<AssessmentItemDto> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, AssessmentItemDto> unique = new LinkedHashMap<>();
        for (AssessmentItemDto item : items) {
            String title = item.getTitle() == null ? "" : item.getTitle().trim().toLowerCase();
            String key = title + "|" + item.getObtainedMarks() + "|" + item.getTotalMarks() + "|" + item.getAbsoluteWeight();
            unique.putIfAbsent(key, item);
        }
        return new ArrayList<>(unique.values());
    }
}
