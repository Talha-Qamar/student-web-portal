package com.studentportal.ui.views;

import com.studentportal.dto.SemesterTranscriptBlock;
import com.studentportal.dto.TranscriptCourseDto;
import com.studentportal.dto.TranscriptResponse;
import com.studentportal.model.UserRole;
import com.studentportal.service.TranscriptService;
import com.studentportal.ui.SessionService;
import com.studentportal.ui.layout.MainLayout;
import com.studentportal.ui.security.AllowedRoles;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "student/transcript", layout = MainLayout.class)
@PageTitle("FAST Portal | Transcript")
@AllowedRoles({UserRole.STUDENT})
public class TranscriptView extends VerticalLayout {

    private final TranscriptService transcriptService;
    private final Grid<TranscriptCourseDto> grid = new Grid<>(TranscriptCourseDto.class, false);
    private final HorizontalLayout summaryRow = new HorizontalLayout();
    private final Div semesterBlocks = new Div();
    private final Div graphWrapper = new Div();
    private final Anchor downloadAnchor = new Anchor();
    private final Checkbox showAllSemesters = new Checkbox("Show upcoming semesters");
    private TranscriptResponse cachedTranscript;
    private Map<Integer, List<TranscriptCourseDto>> coursesBySemester = Collections.emptyMap();

    public TranscriptView(TranscriptService transcriptService) {
        this.transcriptService = transcriptService;
        setSpacing(true);
        setPadding(true);
        setWidthFull();
        getStyle().set("align-items", "center");

        add(new H2("Academic transcript"));
        add(new Paragraph("Finalized semesters only — instantly synced with Neon."));
        getStyle().set("text-align", "center");

        summaryRow.setWidthFull();
        summaryRow.addClassName("page-shell");
        summaryRow.getStyle().set("max-width", "1100px");
        summaryRow.getStyle().set("margin", "0 auto");
        summaryRow.setJustifyContentMode(JustifyContentMode.CENTER);
        summaryRow.addClassName("summary-row");

        configureGrid();
        configureDownloadAnchor();

        showAllSemesters.addValueChangeListener(event -> {
            if (cachedTranscript != null) {
                renderSemesterBlocks(cachedTranscript.getSemesters());
            }
        });
        showAllSemesters.getElement().getThemeList().add("small");

        semesterBlocks.addClassName("cards-grid");
        semesterBlocks.addClassName("page-shell");
        semesterBlocks.getStyle().set("width", "100%");
        semesterBlocks.getStyle().set("max-width", "1100px");
        semesterBlocks.getStyle().set("margin", "0 auto");
        graphWrapper.addClassName("graph-wrapper");
        graphWrapper.addClassName("page-shell");
        graphWrapper.getStyle().set("width", "100%");
        graphWrapper.getStyle().set("max-width", "1100px");
        graphWrapper.getStyle().set("margin", "0 auto");

        grid.setWidthFull();
        grid.addClassName("page-grid-centered");
        grid.getStyle().set("max-width", "1100px");
        grid.getStyle().set("margin", "0 auto");

        add(summaryRow, downloadAnchor, showAllSemesters, graphWrapper, semesterBlocks, grid);
        refresh();
    }

    private void configureDownloadAnchor() {
        Button download = new Button("Download transcript (PDF)");
        download.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        downloadAnchor.add(download);
        downloadAnchor.getElement().setAttribute("download", true);
    }

    private void configureGrid() {
        grid.addColumn(TranscriptCourseDto::getCode).setHeader("Code").setAutoWidth(true);
        grid.addColumn(TranscriptCourseDto::getTitle).setHeader("Title").setFlexGrow(1);
        grid.addColumn(dto -> "Semester " + dto.getSemesterNumber()).setHeader("Semester").setAutoWidth(true);
        grid.addColumn(TranscriptCourseDto::getCreditHours).setHeader("Credits").setAutoWidth(true);
        grid.addColumn(TranscriptCourseDto::getGrade).setHeader("Grade").setAutoWidth(true);
        grid.addColumn(TranscriptCourseDto::getStatus).setHeader("Status").setAutoWidth(true);
        grid.addColumn(dto -> dto.isRepeatRequired() ? "Repeat" : "").setHeader("Repeat").setAutoWidth(true);
        grid.setAllRowsVisible(true);
    }

    private void refresh() {
        Long studentId = SessionService.requireStudentId();
        cachedTranscript = transcriptService.getTranscript(studentId);
        coursesBySemester = cachedTranscript.getCourses().stream()
                .collect(Collectors.groupingBy(TranscriptCourseDto::getSemesterNumber));

        summaryRow.removeAll();
        summaryRow.add(createSummaryCard("Overall GPA", formatNumber(cachedTranscript.getOverallGpa())));
        summaryRow.add(createSummaryCard("Credits", String.valueOf(cachedTranscript.getTotalCredits())));
        summaryRow.add(createSummaryCard("Major", cachedTranscript.getMajor()));

        updateDownloadLink(studentId);
        renderGraph(cachedTranscript.getSemesters());
        renderSemesterBlocks(cachedTranscript.getSemesters());

        grid.setItems(cachedTranscript.getCourses());
    }

    private VerticalLayout createSummaryCard(String label, String value) {
        VerticalLayout card = new VerticalLayout();
        card.addClassName("summary-card");
        card.setAlignItems(Alignment.CENTER);
        card.add(new Paragraph(label));
        card.add(new H2(value));
        return card;
    }

    private void updateDownloadLink(Long studentId) {
        StreamResource resource = new StreamResource(
                "transcript.pdf",
                () -> new ByteArrayInputStream(transcriptService.exportTranscriptPdf(studentId))
        );
        downloadAnchor.setHref(resource);
    }

    private void renderSemesterBlocks(List<SemesterTranscriptBlock> blocks) {
        semesterBlocks.removeAll();
        semesterBlocks.getStyle().set("display", "grid");
        semesterBlocks.getStyle().set("grid-template-columns", "repeat(auto-fit, minmax(280px, 1fr))");
        semesterBlocks.getStyle().set("justify-content", "center");
        if (blocks == null) {
            return;
        }
        Integer currentSemester = SessionService.getCurrentSemester().orElse(1);
        blocks.stream()
                .filter(block -> showAllSemesters.getValue()
                        || block.isFinalized()
                        || block.getSemesterNumber() == currentSemester)
                .forEach(block -> {
                    Details details = buildSemesterDetails(block,
                            coursesBySemester.getOrDefault(block.getSemesterNumber(), List.of()),
                            currentSemester);
                    semesterBlocks.add(details);
                });
    }

    private Details buildSemesterDetails(SemesterTranscriptBlock block,
                                         List<TranscriptCourseDto> courses,
                                         Integer currentSemester) {
        Div summary = new Div();
        summary.addClassName("semester-card");
        if (block.isFinalized()) {
            summary.addClassName("locked");
        } else if (block.getSemesterNumber() == currentSemester) {
            summary.addClassName("active");
        } else {
            summary.addClassName("upcoming");
        }
        summary.add(new H3("Semester " + block.getSemesterNumber()));
        summary.add(new Paragraph(block.isFinalized() ? "Finalized" : "In progress"));
        summary.add(new Paragraph("GPA " + formatNumber(block.getSemesterGpa())));
        summary.add(new Paragraph(block.getCreditsEarned() + " CH"));

        Grid<TranscriptCourseDto> courseTable = buildMiniGrid(courses);
        Details details = new Details(summary, courseTable);
        details.addClassName("semester-details");
        details.setOpened(block.getSemesterNumber() == currentSemester);
        return details;
    }

    private Grid<TranscriptCourseDto> buildMiniGrid(List<TranscriptCourseDto> courses) {
        Grid<TranscriptCourseDto> table = new Grid<>(TranscriptCourseDto.class, false);
        table.addColumn(TranscriptCourseDto::getCode).setHeader("Code").setAutoWidth(true);
        table.addColumn(TranscriptCourseDto::getTitle).setHeader("Title").setFlexGrow(1);
        table.addColumn(TranscriptCourseDto::getCreditHours).setHeader("CH").setAutoWidth(true);
        table.addColumn(TranscriptCourseDto::getGrade).setHeader("Grade").setAutoWidth(true);
        table.addColumn(TranscriptCourseDto::getStatus).setHeader("Status").setAutoWidth(true);
        table.setItems(courses);
        table.setAllRowsVisible(true);
        table.addClassName("mini-transcript-grid");
        table.getElement().setAttribute("theme", "row-dividers compact");
        return table;
    }

    private void renderGraph(List<SemesterTranscriptBlock> blocks) {
        List<Double> gpas = blocks.stream()
                .map(SemesterTranscriptBlock::getSemesterGpa)
                .collect(Collectors.toList());
        String path = buildPath(gpas);
        String svg = "<svg viewBox='0 0 600 140' preserveAspectRatio='none'>"
                + "<polyline fill='none' stroke='var(--lumo-primary-color)' stroke-width='4' points='" + path + "'/>"
                + "</svg>";
        graphWrapper.getElement().setProperty("innerHTML", svg);
    }

    private String buildPath(List<Double> gpas) {
        if (gpas.isEmpty()) {
            return "0,120";
        }
        double maxGpa = 4.0;
        double stepX = 600.0 / Math.max(1, gpas.size() - 1);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < gpas.size(); i++) {
            Double gpa = gpas.get(i) == null ? 0.0 : gpas.get(i);
            double normalized = 120 - (gpa / maxGpa) * 100;
            builder.append(i * stepX).append(",").append(normalized);
            if (i < gpas.size() - 1) {
                builder.append(" ");
            }
        }
        return builder.toString();
    }

    private String formatNumber(Double value) {
        return value == null ? "0.00" : String.format("%.2f", value);
    }
}
