package com.studentportal.ui.views;

import com.studentportal.dto.DropCourseRequest;
import com.studentportal.model.Enrollment;
import com.studentportal.model.EnrollmentStatus;
import com.studentportal.model.UserRole;
import com.studentportal.service.EnrollmentService;
import com.studentportal.ui.SessionService;
import com.studentportal.ui.layout.MainLayout;
import com.studentportal.ui.security.AllowedRoles;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "student/enrollments", layout = MainLayout.class)
@PageTitle("FAST Portal | Enrollments")
@AllowedRoles({UserRole.STUDENT})
public class EnrollmentsView extends VerticalLayout {

    private final EnrollmentService enrollmentService;
    private final Grid<Enrollment> grid = new Grid<>(Enrollment.class, false);
    private final Checkbox showDropped = new Checkbox("Show dropped courses");
    private final Div actionOverlay = new Div();
    private final Div gridWrapper = new Div();

    public EnrollmentsView(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
        setSpacing(true);
        setPadding(true);
        setWidthFull();
        getStyle().set("align-items", "center");

        VerticalLayout headerSection = new VerticalLayout();
        headerSection.setSpacing(false);
        headerSection.setPadding(false);
        headerSection.setWidthFull();
        headerSection.addClassName("page-header-centered");
        headerSection.addClassName("page-shell");
        headerSection.getStyle().set("max-width", "1100px");
        headerSection.getStyle().set("margin", "0 auto");
        headerSection.getStyle().set("text-align", "center");
        headerSection.add(new H2("My enrollments"));
        headerSection.add(new Paragraph("Track every registered course and drop when needed."));
        add(headerSection);

        showDropped.addValueChangeListener(event -> refresh());
        showDropped.getElement().getThemeList().add("small");
        HorizontalLayout toolbar = new HorizontalLayout(showDropped);
        toolbar.setWidthFull();
        toolbar.addClassName("page-controls-centered");
        toolbar.addClassName("page-shell");
        toolbar.getStyle().set("max-width", "1100px");
        toolbar.getStyle().set("margin", "0 auto");
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.setJustifyContentMode(JustifyContentMode.CENTER);
        add(toolbar);

        configureGrid();
        configureOverlay();
        gridWrapper.addClassName("grid-overlay-wrapper");
        gridWrapper.addClassName("page-shell");
        gridWrapper.setWidthFull();
        gridWrapper.getStyle().set("max-width", "1100px");
        gridWrapper.getStyle().set("margin", "0 auto");
        gridWrapper.add(grid, actionOverlay);
        add(gridWrapper);
        refresh();
    }

    private void configureOverlay() {
        actionOverlay.addClassName("action-overlay");
        actionOverlay.add(new Paragraph("Processing request..."));
        actionOverlay.setVisible(false);
    }

    private void configureGrid() {
        grid.addColumn(enrollment -> enrollment.getCourse().getCode()).setHeader("Code").setAutoWidth(true);
        grid.addColumn(enrollment -> enrollment.getCourse().getTitle()).setHeader("Title").setFlexGrow(1);
        grid.addColumn(enrollment -> enrollment.getCourse().getTerm()).setHeader("Term").setAutoWidth(true);
        grid.addColumn(enrollment -> enrollment.getStatus().name()).setHeader("Status").setAutoWidth(true);
        grid.addColumn(Enrollment::getGrade).setHeader("Grade").setAutoWidth(true);
        grid.addComponentColumn(enrollment -> buildRepeatBadge(enrollment.isRepeatRequired())).setHeader("Repeat")
                .setAutoWidth(true);
        grid.addComponentColumn(this::buildDropButton).setHeader("Actions").setAutoWidth(true);
        grid.setWidthFull();
        grid.setAllRowsVisible(true);
    }

    private Div buildRepeatBadge(boolean repeatRequired) {
        Div badge = new Div();
        badge.setText(repeatRequired ? "Required" : "");
        badge.setClassName("repeat-badge");
        return badge;
    }

    private Button buildDropButton(Enrollment enrollment) {
        Button button = new Button("Drop", event -> confirmDrop(enrollment));
        button.addThemeVariants(ButtonVariant.LUMO_ERROR);
        boolean droppable = EnrollmentStatus.ENROLLED.equals(enrollment.getStatus())
                && (enrollment.getGrade() == null || enrollment.getGrade().isBlank());
        button.setEnabled(droppable);
        return button;
    }

    private void confirmDrop(Enrollment enrollment) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Drop " + enrollment.getCourse().getCode());
        dialog.setText("Are you sure you want to drop this course? Locked semesters cannot be modified later.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Drop course");
        dialog.addConfirmListener(event -> drop(enrollment));
        dialog.open();
    }

    private void drop(Enrollment enrollment) {
        try {
            toggleOverlay(true);
            DropCourseRequest request = new DropCourseRequest();
            request.setStudentId(SessionService.requireStudentId());
            request.setCourseId(enrollment.getCourse().getId());
            enrollmentService.dropCourse(request);
            Notification success = Notification.show("Dropped " + enrollment.getCourse().getCode());
            success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            triggerHaptics(true);
            refresh();
        } catch (Exception ex) {
            Notification error = Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            triggerHaptics(false);
        } finally {
            toggleOverlay(false);
        }
    }

    private void refresh() {
        List<Enrollment> items = enrollmentService.getEnrollmentsForStudent(SessionService.requireStudentId());
        if (!showDropped.getValue()) {
            items = items.stream()
                    .filter(enrollment -> EnrollmentStatus.ENROLLED.equals(enrollment.getStatus()))
                    .toList();
        }
        grid.setItems(items);
    }

    private void triggerHaptics(boolean success) {
        getElement().executeJs("if (navigator && navigator.vibrate) { navigator.vibrate($0); }", success ? 15 : 70);
    }

    private void toggleOverlay(boolean visible) {
        actionOverlay.setVisible(visible);
    }
}
