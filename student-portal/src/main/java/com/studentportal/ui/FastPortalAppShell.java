package com.studentportal.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * Declares Vaadin theme metadata outside of {@code @SpringBootApplication} class, per Vaadin requirements.
 */
@Theme(value = "fast-portal", variant = Lumo.DARK)
public class FastPortalAppShell implements AppShellConfigurator {
}
