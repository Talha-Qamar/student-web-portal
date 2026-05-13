package com.studentportal.ui.layout;

import com.studentportal.model.UserRole;
import com.studentportal.ui.UiTestSupport;
import com.studentportal.ui.views.AdminDashboardView;
import com.studentportal.ui.views.DashboardView;
import com.studentportal.ui.views.RoleSelectionView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.theme.lumo.Lumo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainLayoutTest {

    @AfterEach
    void tearDown() {
        UiTestSupport.clear();
    }

    @Test
    void toggleThemeFlipsBetweenDarkAndLight() throws Exception {
        // Covers the theme-toggle branch for both dark-to-light and light-to-dark transitions.
        UiTestSupport.attachSession(UserRole.STUDENT, "Ayesha Khan");
        UI ui = UiTestSupport.attachUi();
        ui.getElement().getThemeList().add(Lumo.DARK);

        MainLayout layout = new MainLayout();
        Method toggleTheme = MainLayout.class.getDeclaredMethod("toggleTheme");
        toggleTheme.setAccessible(true);

        toggleTheme.invoke(layout);
        assertTrue(ui.getElement().getThemeList().contains(Lumo.LIGHT));
        assertFalse(ui.getElement().getThemeList().contains(Lumo.DARK));

        toggleTheme.invoke(layout);
        assertTrue(ui.getElement().getThemeList().contains(Lumo.DARK));
    }

    @Test
    void beforeEnterReroutesGuestsAndMismatchedRolesButAllowsMatchedRoles() {
        // Covers the guest reroute branch, allowed-role no-reroute branch, and mismatched-role reroute branch.
        UiTestSupport.attachSession(null, null);
        MainLayout layout = new MainLayout();
        BeforeEnterEvent event = newEvent(DashboardView.class);

        layout.beforeEnter(event);
        assertTrue(event.hasRerouteTarget());
        assertEquals(RoleSelectionView.class, event.getRerouteTargetType());

        UiTestSupport.attachSession(UserRole.STUDENT, "Ayesha Khan");
        BeforeEnterEvent allowedEvent = newEvent(DashboardView.class);
        layout.beforeEnter(allowedEvent);
        assertFalse(allowedEvent.hasRerouteTarget());

        BeforeEnterEvent mismatchedEvent = newEvent(AdminDashboardView.class);
        layout.beforeEnter(mismatchedEvent);
        assertTrue(mismatchedEvent.hasRerouteTarget());
        assertEquals(DashboardView.class, mismatchedEvent.getRerouteTargetType());
    }

    private BeforeEnterEvent newEvent(Class<?> target) {
        UI ui = UiTestSupport.attachUi();
        Router router = org.mockito.Mockito.mock(Router.class);
        return new BeforeEnterEvent(router, NavigationTrigger.PROGRAMMATIC, new Location(""), target, ui, List.of());
    }
}