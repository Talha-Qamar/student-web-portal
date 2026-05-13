package com.studentportal.ui.layout;

import com.studentportal.model.UserRole;
import com.studentportal.ui.SessionService;
import com.studentportal.ui.security.AllowedRoles;
import com.studentportal.ui.security.RoleRouteResolver;
import com.studentportal.ui.views.AdminAssignmentView;
import com.studentportal.ui.views.AdminDashboardView;
import com.studentportal.ui.views.AdminManageUsersView;
import com.studentportal.ui.views.AttendanceView;
import com.studentportal.ui.views.CoursesView;
import com.studentportal.ui.views.DashboardView;
import com.studentportal.ui.views.EnrollmentsView;
import com.studentportal.ui.views.FacultyAttendanceUploadView;
import com.studentportal.ui.views.FacultyDashboardView;
import com.studentportal.ui.views.FacultyFeedbackInsightsView;
import com.studentportal.ui.views.FacultyGradeUploadView;
import com.studentportal.ui.views.FeeChallanView;
import com.studentportal.ui.views.MarksView;
import com.studentportal.ui.views.RoleSelectionView;
import com.studentportal.ui.views.StudentFeedbackView;
import com.studentportal.ui.views.TranscriptView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.theme.lumo.Lumo;

public class MainLayout extends AppLayout implements BeforeEnterObserver {

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();
        UserRole role = SessionService.getRole().orElse(UserRole.STUDENT);

        Image logo = new Image("/images/logo.png", "FAST logo");
        logo.addClassName("header-logo");

        String portalTitle = switch (role) {
            case FACULTY -> "FAST Faculty Portal";
            case ADMIN -> "FAST Admin Portal";
            default -> "FAST Student Portal";
        };

        Div brand = new Div(logo, new H1(portalTitle));
        brand.addClassName("header-brand");

        Span userChip = new Span(SessionService.getStudentName().orElse(""));
        userChip.getElement().getThemeList().add("badge contrast");

        Button themeToggle = new Button(new Icon(VaadinIcon.ADJUST), event -> toggleTheme());
        themeToggle.addClassName("theme-toggle");
        themeToggle.getElement().setProperty("title", "Toggle theme");

        Button logout = new Button("Sign out", event -> {
            SessionService.clear();
            getUI().ifPresent(ui -> ui.navigate(RoleSelectionView.class));
        });
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        Div spacer = new Div();
        HorizontalLayout header = new HorizontalLayout(toggle, brand, spacer, themeToggle, userChip, logout);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.expand(spacer);
        header.setWidthFull();
        header.addClassName("portal-header");

        addToNavbar(header);
    }

    private void createDrawer() {
        UserRole role = SessionService.getRole().orElse(UserRole.STUDENT);
        VerticalLayout brand = new VerticalLayout();
        brand.addClassName("drawer-brand");
        Image logo = new Image("/images/logo.png", "FAST crest");
        logo.setWidth("72px");
        logo.setHeight("72px");
        Span label = new Span(role.name().charAt(0) + role.name().substring(1).toLowerCase() + " Portal");
        brand.add(logo, label);
        brand.setSpacing(false);
        brand.setPadding(false);
        brand.setAlignItems(Alignment.CENTER);

        VerticalLayout nav = new VerticalLayout();
        nav.addClassName("drawer-nav");
        if (role == UserRole.STUDENT) {
            nav.add(createLink("Dashboard", VaadinIcon.DASHBOARD, DashboardView.class));
            nav.add(createLink("Course catalog", VaadinIcon.BOOK, CoursesView.class));
            nav.add(createLink("My enrollments", VaadinIcon.CLIPBOARD_CHECK, EnrollmentsView.class));
            nav.add(createLink("Attendance", VaadinIcon.CALENDAR_CLOCK, AttendanceView.class));
            nav.add(createLink("Marks", VaadinIcon.BAR_CHART_H, MarksView.class));
            nav.add(createLink("Transcript", VaadinIcon.FILE_TEXT, TranscriptView.class));
            nav.add(createLink("Fee challan", VaadinIcon.CREDIT_CARD, FeeChallanView.class));
            nav.add(createLink("Faculty feedback", VaadinIcon.COMMENTS, StudentFeedbackView.class));
        } else if (role == UserRole.FACULTY) {
            nav.add(createLink("Dashboard", VaadinIcon.DASHBOARD, FacultyDashboardView.class));
            nav.add(createLink("Anonymous feedback", VaadinIcon.COMMENTS, FacultyFeedbackInsightsView.class));
            nav.add(createLink("Upload grades", VaadinIcon.BAR_CHART_H, FacultyGradeUploadView.class));
            nav.add(createLink("Upload attendance", VaadinIcon.CALENDAR_CLOCK, FacultyAttendanceUploadView.class));
        } else if (role == UserRole.ADMIN) {
            nav.add(createLink("Dashboard", VaadinIcon.DASHBOARD, AdminDashboardView.class));
            nav.add(createLink("Assign instructor", VaadinIcon.BOOK, AdminAssignmentView.class));
            nav.add(createLink("Edit user details", VaadinIcon.USER, AdminManageUsersView.class));
        }
        nav.setSpacing(false);
        nav.setPadding(false);

        Scroller scroller = new Scroller(nav);
        scroller.addClassName("drawer-scroller");
        addToDrawer(brand, scroller);
    }

    private RouterLink createLink(String text, VaadinIcon icon,
                                  Class<? extends com.vaadin.flow.component.Component> target) {
        RouterLink link = new RouterLink();
        if (VaadinService.getCurrent() != null && VaadinService.getCurrent().getRouter() != null) {
            link.setRoute(target);
        }
        Icon iconComponent = icon.create();
        iconComponent.addClassName("nav-icon");
        Span label = new Span(text);
        Div content = new Div(iconComponent, label);
        content.addClassName("nav-link-content");
        link.add(content);
        link.addClassName("nav-link");
        link.setHighlightCondition(HighlightConditions.sameLocation());
        link.setHighlightAction((routerLink, highlight) -> {
            if (highlight) {
                routerLink.addClassName("active");
            } else {
                routerLink.removeClassName("active");
            }
        });
        return link;
    }

    private void toggleTheme() {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return;
        }
        var themeList = ui.getElement().getThemeList();
        if (themeList.contains(Lumo.DARK)) {
            themeList.remove(Lumo.DARK);
            themeList.add(Lumo.LIGHT);
        } else {
            themeList.remove(Lumo.LIGHT);
            themeList.add(Lumo.DARK);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!SessionService.isAuthenticated()) {
            event.rerouteTo(RoleSelectionView.class);
            return;
        }

        AllowedRoles allowedRoles = event.getNavigationTarget().getAnnotation(AllowedRoles.class);
        if (allowedRoles == null) {
            return;
        }
        UserRole currentRole = SessionService.getRole().orElse(null);
        for (UserRole allowedRole : allowedRoles.value()) {
            if (allowedRole == currentRole) {
                return;
            }
        }
        Class<? extends Component> target = RoleRouteResolver.homeFor(currentRole);
        event.rerouteTo(target);
    }
}
