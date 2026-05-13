package com.studentportal.ui.views;

import com.studentportal.dto.RegisterCourseRequest;
import com.studentportal.model.Course;
import com.studentportal.model.UserRole;
import com.studentportal.service.CourseService;
import com.studentportal.service.EnrollmentService;
import com.studentportal.service.TranscriptService;
import com.studentportal.ui.SessionService;
import com.studentportal.ui.layout.MainLayout;
import com.studentportal.ui.security.AllowedRoles;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "student/courses", layout = MainLayout.class)
@PageTitle("FAST Portal | Courses")
@AllowedRoles({UserRole.STUDENT})
public class CoursesView extends VerticalLayout {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final TranscriptService transcriptService;
    private final Grid<Course> grid = new Grid<>(Course.class, false);
    private final TextField searchField = new TextField();
    private final Select<String> semesterFilter = new Select<>();
    private final Checkbox backlogOnly = new Checkbox("Backlog only");
    private final Checkbox showAllSemesterTiles = new Checkbox("Show upcoming semesters");
    private final Div semesterCards = new Div();
    private final Div actionOverlay = new Div();
    private final Div gridWrapper = new Div();
    private List<Course> cachedCourses = new ArrayList<>();

    public CoursesView(CourseService courseService,
                       EnrollmentService enrollmentService,
                       TranscriptService transcriptService) {
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.transcriptService = transcriptService;

        setSpacing(true);
        setPadding(true);
        setWidthFull();
        getStyle().set("align-items", "center");
        addClassName("courses-grid");

        VerticalLayout headerSection = new VerticalLayout();
        headerSection.setSpacing(false);
        headerSection.setPadding(false);
        headerSection.setWidthFull();
        headerSection.addClassName("page-header-centered");
        headerSection.addClassName("page-shell");
        headerSection.getStyle().set("max-width", "1100px");
        headerSection.getStyle().set("margin", "0 auto");
        headerSection.getStyle().set("text-align", "center");
        headerSection.add(new H2("Course catalog"));
        headerSection.add(new Paragraph("Register instantly — seats update in real time."));
        add(headerSection);

        configureControls();
        configureGrid();
        configureOverlay();
        gridWrapper.addClassName("grid-overlay-wrapper");
        gridWrapper.addClassName("page-shell");
        gridWrapper.setWidthFull();
        gridWrapper.getStyle().set("max-width", "1100px");
        gridWrapper.getStyle().set("margin", "0 auto");
        gridWrapper.add(grid, actionOverlay);
        add(showAllSemesterTiles, semesterCards, gridWrapper);
        refresh();
    }

    private void configureOverlay() {
        actionOverlay.addClassName("action-overlay");
        actionOverlay.add(new Paragraph("Processing request..."));
        actionOverlay.setVisible(false);
    }

    private void configureControls() {
        searchField.setPlaceholder("Search code or title");
        searchField.setClearButtonVisible(true);
        searchField.addValueChangeListener(event -> applyFilters());

        semesterFilter.setLabel("Semester");
        semesterFilter.setItems("All", "1", "2", "3", "4", "5", "6", "7", "8");
        semesterFilter.setValue("All");
        semesterFilter.addValueChangeListener(event -> applyFilters());

        backlogOnly.addValueChangeListener(event -> applyFilters());

        showAllSemesterTiles.addValueChangeListener(event -> populateSemesterCards());
        showAllSemesterTiles.getElement().getThemeList().add("small");

        HorizontalLayout controls = new HorizontalLayout(searchField, semesterFilter, backlogOnly);
        controls.setWidthFull();
        controls.addClassName("page-controls-centered");
        controls.addClassName("page-shell");
        controls.getStyle().set("max-width", "1100px");
        controls.getStyle().set("margin", "0 auto");
        controls.setAlignItems(Alignment.END);
        controls.setJustifyContentMode(JustifyContentMode.CENTER);
        add(controls);
    }

    private void configureGrid() {
        grid.addColumn(Course::getCode).setHeader("Code").setAutoWidth(true);
        grid.addColumn(Course::getTitle).setHeader("Title").setFlexGrow(1);
        grid.addColumn(course -> "Semester " + course.getSemesterNumber()).setHeader("Semester").setAutoWidth(true);
        grid.addColumn(course -> course.getPrerequisite() != null ? course.getPrerequisite().getCode() : "None")
                .setHeader("Prerequisite").setAutoWidth(true);
        grid.addColumn(Course::getCreditHours).setHeader("Credits").setAutoWidth(true);
        grid.addComponentColumn(this::buildStatusBadge).setHeader("Track").setAutoWidth(true);
        grid.addColumn(course -> course.getEnrolledCount() + "/" + course.getCapacity()).setHeader("Fill").setAutoWidth(true);
        grid.addComponentColumn(this::buildRegisterButton).setHeader("Actions").setAutoWidth(true).setFlexGrow(0);
        grid.setWidthFull();
        grid.setAllRowsVisible(true);
    }

    private Div buildStatusBadge(Course course) {
        Div badge = new Div();
        badge.getStyle().set("padding", "0.15rem 0.6rem");
        badge.getStyle().set("border-radius", "999px");
        boolean backlog = isBacklogCourse(course);
        badge.getStyle().set("background", backlog ? "rgba(244,67,54,0.18)" : "rgba(76,175,80,0.18)");
        badge.getStyle().set("color", backlog ? "#f44336" : "#4caf50");
        badge.add(new Text(backlog ? "Backlog" : "Current"));
        return badge;
    }

    private Button buildRegisterButton(Course course) {
        Button button = new Button("Register");
        button.addClickListener(event -> registerCourse(course, button));
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.setEnabled(course.getCapacity() == null || course.getEnrolledCount() < course.getCapacity());
        return button;
    }

    private void registerCourse(Course course, Button trigger) {
        try {
            trigger.setEnabled(false);
            toggleOverlay(true);
            RegisterCourseRequest request = new RegisterCourseRequest();
            request.setStudentId(SessionService.requireStudentId());
            request.setCourseId(course.getId());
            enrollmentService.registerCourse(request);
            Notification success = Notification.show("Registered in " + course.getCode());
            success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            triggerHaptics(true);
            refresh();
        } catch (Exception ex) {
            Notification error = Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            triggerHaptics(false);
        } finally {
            trigger.setEnabled(true);
            toggleOverlay(false);
        }
    }

    private void toggleOverlay(boolean visible) {
        actionOverlay.setVisible(visible);
    }

    private void refresh() {
        Long studentId = SessionService.requireStudentId();
        cachedCourses = courseService.getAvailableCoursesForStudent(studentId);
        populateSemesterCards();
        applyFilters();
    }

    private void applyFilters() {
        String needle = searchField.getValue() == null ? "" : searchField.getValue().trim().toLowerCase();
        String semesterSelection = semesterFilter.getValue();

        List<Course> filtered = cachedCourses.stream()
                .filter(course -> needle.isBlank()
                        || course.getCode().toLowerCase().contains(needle)
                        || course.getTitle().toLowerCase().contains(needle))
                .filter(course -> {
                    if (semesterSelection == null || "All".equals(semesterSelection)) {
                        return true;
                    }
                    return String.valueOf(course.getSemesterNumber()).equals(semesterSelection);
                })
                .filter(course -> !backlogOnly.getValue() || isBacklogCourse(course))
                .collect(Collectors.toList());

        grid.setItems(filtered);
    }

    private boolean isBacklogCourse(Course course) {
        Integer currentSemester = SessionService.getCurrentSemester().orElse(1);
        return course.getSemesterNumber() != null && course.getSemesterNumber() < currentSemester;
    }

    private void populateSemesterCards() {
        semesterCards.removeAll();
        semesterCards.addClassName("cards-grid");
        semesterCards.addClassName("page-shell");
        semesterCards.getStyle().set("display", "grid");
        semesterCards.getStyle().set("grid-template-columns", "repeat(auto-fit, minmax(220px, 1fr))");
        semesterCards.getStyle().set("width", "100%");
        semesterCards.getStyle().set("max-width", "1100px");
        semesterCards.getStyle().set("margin", "0 auto");
        semesterCards.getStyle().set("justify-content", "center");

        Map<Integer, List<Course>> catalog = courseService.getCatalogBySemester();
        Integer currentSemester = SessionService.getCurrentSemester().orElse(1);
        transcriptService.getTranscript(SessionService.requireStudentId())
                .getSemesters()
                .stream()
                .filter(block -> showAllSemesterTiles.getValue()
                        || block.isFinalized()
                        || block.getSemesterNumber() == currentSemester)
                .forEach(block -> {
                    Div card = new Div();
                    card.addClassName("semester-card");
                    if (block.isFinalized()) {
                        card.addClassName("locked");
                    } else if (block.getSemesterNumber() == currentSemester) {
                        card.addClassName("active");
                    } else if (block.getSemesterNumber() > currentSemester) {
                        card.addClassName("upcoming");
                    }
                    card.add(new Paragraph("Semester " + block.getSemesterNumber()));
                    card.add(new Paragraph(block.isFinalized() ? "Finalized" : "Open"));
                    List<Course> semCourses = catalog.getOrDefault(block.getSemesterNumber(), List.of());
                    card.add(new Paragraph(semCourses.size() + " courses"));
                    card.addClickListener(event -> {
                        semesterFilter.setValue(String.valueOf(block.getSemesterNumber()));
                        applyFilters();
                    });
                    semesterCards.add(card);
                });
    }

    private void triggerHaptics(boolean success) {
        getElement().executeJs("if (navigator && navigator.vibrate) { navigator.vibrate($0); }", success ? 20 : 60);
    }
}
