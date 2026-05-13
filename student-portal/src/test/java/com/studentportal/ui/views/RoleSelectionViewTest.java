package com.studentportal.ui.views;

import com.studentportal.model.UserRole;
import com.studentportal.ui.UiTestSupport;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.component.UI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleSelectionViewTest {

    @AfterEach
    void tearDown() {
        UiTestSupport.clear();
    }

    @Test
    void beforeEnterLeavesGuestsAndRedirectsAuthenticatedUsers() {
        // Covers the unauthenticated no-reroute path and the authenticated reroute path.
        RoleSelectionView view = new RoleSelectionView();
        BeforeEnterEvent event = newEvent();

        view.beforeEnter(event);
        assertFalse(event.hasRerouteTarget());

        UiTestSupport.attachSession(UserRole.FACULTY, "Dr. Sara");
        BeforeEnterEvent authenticatedEvent = newEvent();
        view.beforeEnter(authenticatedEvent);
        assertTrue(authenticatedEvent.hasRerouteTarget());
        assertEquals(FacultyDashboardView.class, authenticatedEvent.getRerouteTargetType());
    }

    private BeforeEnterEvent newEvent() {
        UI ui = UiTestSupport.attachUi();
        Router router = org.mockito.Mockito.mock(Router.class);
        return new BeforeEnterEvent(router, NavigationTrigger.PROGRAMMATIC, new Location(""), RoleSelectionView.class, ui, java.util.List.of());
    }
}