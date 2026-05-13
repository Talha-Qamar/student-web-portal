package com.studentportal.ui.views;

import com.studentportal.model.UserRole;
import com.studentportal.ui.SessionService;
import com.studentportal.ui.security.RoleRouteResolver;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("")
@PageTitle("FAST Portal | Division Selection")
public class RoleSelectionView extends VerticalLayout implements BeforeEnterObserver {

    public RoleSelectionView() {
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        addClassName("login-view");

        Div card = new Div();
        card.addClassName("login-card");
        card.getStyle().set("max-width", "420px");
        card.getStyle().set("width", "100%");
        card.getStyle().set("display", "flex");
        card.getStyle().set("flex-direction", "column");
        card.getStyle().set("align-items", "center");
        card.getStyle().set("text-align", "center");
        card.getStyle().set("gap", "0.8rem");

        Image logo = new Image("/images/logo.png", "FAST crest");
        logo.addClassName("login-logo");

        H1 title = new H1("FAST University Portal");
        title.getStyle().set("margin", "0.3rem 0 0 0");
        title.getStyle().set("font-size", "1.45rem");
        title.getStyle().set("text-align", "center");

        Paragraph subtitle = new Paragraph("Choose how you want to enter");
        subtitle.getStyle().set("margin-top", "0");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");
        subtitle.getStyle().set("text-align", "center");
        subtitle.getStyle().set("width", "100%");

        VerticalLayout options = new VerticalLayout();
        options.setPadding(false);
        options.setSpacing(true);
        options.setWidthFull();
        options.getStyle().set("margin-top", "0.7rem");

        options.add(buildRoleButton(UserRole.STUDENT, "Enter As Student"));
        options.add(buildRoleButton(UserRole.FACULTY, "Enter As Faculty"));
        options.add(buildRoleButton(UserRole.ADMIN, "Enter As Admin"));

        card.add(logo, title, subtitle, options);
        add(card);
    }

    private Button buildRoleButton(UserRole role, String label) {
        Button button = new Button(label, event ->
                getUI().ifPresent(ui -> ui.navigate("login/" + role.name().toLowerCase())));
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.setWidthFull();
        button.getStyle().set("height", "46px");
        button.getStyle().set("font-weight", "600");
        return button;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (SessionService.isAuthenticated()) {
            event.rerouteTo(RoleRouteResolver.homeFor(SessionService.getRole().orElse(null)));
        }
    }
}
