package com.studentportal.ui.views;

import com.studentportal.dto.SemesterTranscriptBlock;
import com.studentportal.model.EnrollmentStatus;
import com.studentportal.model.Enrollment;
import com.studentportal.model.UserRole;
import com.studentportal.service.CourseService;
import com.studentportal.service.EnrollmentService;
import com.studentportal.service.TranscriptService;
import com.studentportal.ui.SessionService;
import com.studentportal.ui.layout.MainLayout;
import com.studentportal.ui.security.AllowedRoles;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.Collections;
import java.util.List;

@Route(value = "student/dashboard", layout = MainLayout.class)
@PageTitle("FAST Portal | Dashboard")
@AllowedRoles({UserRole.STUDENT})
public class DashboardView extends VerticalLayout {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final TranscriptService transcriptService;
    private final Div semesterBoard = new Div();
    private final Checkbox showAllSemesters = new Checkbox("Show upcoming semesters");
    private List<SemesterTranscriptBlock> cachedSemesters = Collections.emptyList();

    public DashboardView(CourseService courseService,
                         EnrollmentService enrollmentService,
                         TranscriptService transcriptService) {
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.transcriptService = transcriptService;

        setSpacing(true);
        setPadding(true);
        addClassName("dashboard-view");

        showAllSemesters.addValueChangeListener(event -> renderSemesterCards());
        showAllSemesters.getElement().getThemeList().add("small");
        semesterBoard.addClassName("semester-board");
        add(showAllSemesters, semesterBoard);

        render();
    }

    private void render() {
        Long studentId = SessionService.requireStudentId();

        var courses = courseService.getAllCourses();
        var enrollments = enrollmentService.getEnrollmentsForStudent(studentId);
        var transcript = transcriptService.getTranscript(studentId);

        add(new H2("Salaam, " + SessionService.getStudentName().orElse("Student")));
        add(new Paragraph("Track catalog, enrollments, and transcript status in one glance."));

        HorizontalLayout statsRow = new HorizontalLayout();
        statsRow.setWidthFull();
        statsRow.add(createStatCard("Catalog size", courses.size(), "Courses available"));
        statsRow.add(createStatCard("Active enrollments", (int) enrollments.stream()
            .filter(e -> EnrollmentStatus.ENROLLED.equals(e.getStatus())).count(), "Currently registered"));
        statsRow.add(createStatCard("Backlog", enrollments.stream().filter(Enrollment::isRepeatRequired).count(), "Repeat required"));
        statsRow.add(createStatCard("Overall GPA", transcript.getOverallGpa() != null ? transcript.getOverallGpa() : 0.0, "Finalized semesters"));
        add(statsRow);

        cachedSemesters = transcript.getSemesters();
        renderSemesterCards();
    }

    private void renderSemesterCards() {
        semesterBoard.removeAll();
        Integer currentSemester = SessionService.getCurrentSemester().orElse(1);
        cachedSemesters.stream()
                .filter(block -> showAllSemesters.getValue()
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
                    card.add(new H3("Semester " + block.getSemesterNumber()));
                    card.add(new Paragraph(block.isFinalized() ? "Locked" : (block.getSemesterNumber() == currentSemester ? "Now open" : "Upcoming")));
                    card.add(new Paragraph("GPA " + formatNumber(block.getSemesterGpa())));
                    card.add(new Paragraph(block.getCreditsEarned() + " CH"));
                    semesterBoard.add(card);
                });
    }

    private String formatNumber(Double value) {
        return value == null ? "0.00" : String.format("%.2f", value);
    }

    private VerticalLayout createStatCard(String title, Number value, String subtitle) {
        VerticalLayout card = new VerticalLayout();
        card.addClassName("stat-card");
        card.add(new Text(title));
        H2 metric = new H2(String.valueOf(value));
        card.add(metric);
        card.add(new Paragraph(subtitle));
        return card;
    }
}
