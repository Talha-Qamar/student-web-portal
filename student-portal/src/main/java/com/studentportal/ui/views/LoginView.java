package com.studentportal.ui.views;

import com.studentportal.dto.LoginRequest;
import com.studentportal.dto.LoginResponse;
import com.studentportal.model.UserRole;
import com.studentportal.service.AuthService;
import com.studentportal.ui.SessionService;
import com.studentportal.ui.security.RoleRouteResolver;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;

@Route(value = "login")
@PageTitle("FAST Portal | Login")
public class LoginView extends VerticalLayout implements BeforeEnterObserver, HasUrlParameter<String> {

    private final AuthService authService;

    private final EmailField emailField = new EmailField("FAST email");
    private final PasswordField passwordField = new PasswordField("Password");
    private UserRole selectedRole;
    private final H1 title = new H1();
    private final Paragraph subtitle = new Paragraph();

    public LoginView(AuthService authService) {
        this.authService = authService;
        setSizeFull();
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        addClassName("login-view");

        emailField.setWidth("320px");
        passwordField.setWidth("320px");
        emailField.setPlaceholder("sara.naveed@fast.edu");
        passwordField.setPlaceholder("••••••••");

        Button submit = new Button("Enter portal", event -> authenticate());
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.setWidthFull();

        FormLayout form = new FormLayout(emailField, passwordField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.setWidth("340px");

        Image logo = new Image("/images/logo.png", "FAST crest");
        logo.addClassName("login-logo");

        VerticalLayout card = new VerticalLayout(
            logo,
            title,
            subtitle,
            form,
            submit
        );
        card.setWidth("380px");
        card.setPadding(true);
        card.setSpacing(true);
        card.setAlignItems(Alignment.CENTER);
        card.getStyle().set("text-align", "center");
        card.addClassName("login-card");

        add(card);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        if (parameter == null || parameter.isBlank()) {
            selectedRole = null;
            title.setText("Select division");
            subtitle.setText("Go back and choose Student, Faculty, or Admin first.");
            return;
        }
        try {
            selectedRole = UserRole.fromValue(parameter);
            title.setText("FAST " + capitalize(selectedRole.name()) + " Login");
            subtitle.setText(switch (selectedRole) {
                case STUDENT -> "Track academics, enroll courses, and monitor transcripts.";
                case FACULTY -> "Upload attendance and grades for assigned course-sections.";
                case ADMIN -> "Manage course assignments and user details.";
            });
        } catch (IllegalArgumentException ex) {
            selectedRole = null;
            title.setText("Invalid division");
            subtitle.setText("This login link is not valid. Please choose a division again.");
        }
    }

    private void authenticate() {
        if (selectedRole == null) {
            Notification.show("Select a valid division before signing in.");
            return;
        }
        try {
            LoginRequest request = new LoginRequest();
            request.setRole(selectedRole.name());
            request.setEmail(emailField.getValue() != null ? emailField.getValue().trim() : null);
            request.setPassword(passwordField.getValue());
            LoginResponse response = authService.login(request);
            SessionService.store(response);
            Notification.show("Assalamualaikum " + response.getFullName());
            UserRole roleToRoute = response.getRole() != null ? response.getRole() : selectedRole;
            Class<? extends Component> target = RoleRouteResolver.homeFor(roleToRoute);
            String targetUrl = RouteConfiguration.forSessionScope().getUrl(target);
            getUI().ifPresent(ui -> ui.getPage().setLocation(targetUrl));
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (SessionService.isAuthenticated()) {
            event.rerouteTo(RoleRouteResolver.homeFor(SessionService.getRole().orElse(null)));
        }
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String lower = value.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
