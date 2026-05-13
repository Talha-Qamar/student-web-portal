package com.studentportal.ui.views;

import com.studentportal.model.UserRole;
import com.studentportal.service.AuthService;
import com.studentportal.ui.UiTestSupport;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.Router;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class LoginViewTest {

    @Mock
    private AuthService authService;

    @AfterEach
    void tearDown() {
        UiTestSupport.clear();
    }

    @Test
    void setParameterHandlesBlankInvalidAndValidRoles() {
        // Covers the blank-parameter branch, invalid-role branch, and all valid role subtitle branches.
        LoginView view = new LoginView(authService);

        view.setParameter(null, null);
        assertAll(
                () -> assertNull(ReflectionTestUtils.getField(view, "selectedRole")),
                () -> assertEquals("Select division", ((H1) ReflectionTestUtils.getField(view, "title")).getText()),
                () -> assertEquals("Go back and choose Student, Faculty, or Admin first.", ((Paragraph) ReflectionTestUtils.getField(view, "subtitle")).getText())
        );

        view.setParameter(null, "invalid");
        assertAll(
                () -> assertNull(ReflectionTestUtils.getField(view, "selectedRole")),
                () -> assertEquals("Invalid division", ((H1) ReflectionTestUtils.getField(view, "title")).getText()),
                () -> assertEquals("This login link is not valid. Please choose a division again.", ((Paragraph) ReflectionTestUtils.getField(view, "subtitle")).getText())
        );

        view.setParameter(null, "faculty");
        assertAll(
                () -> assertEquals(UserRole.FACULTY, ReflectionTestUtils.getField(view, "selectedRole")),
                () -> assertEquals("FAST Faculty Login", ((H1) ReflectionTestUtils.getField(view, "title")).getText()),
                () -> assertEquals("Upload attendance and grades for assigned course-sections.", ((Paragraph) ReflectionTestUtils.getField(view, "subtitle")).getText())
        );

        view.setParameter(null, "student");
        assertEquals("FAST Student Login", ((H1) ReflectionTestUtils.getField(view, "title")).getText());
        view.setParameter(null, "admin");
        assertEquals("FAST Admin Login", ((H1) ReflectionTestUtils.getField(view, "title")).getText());
    }

    @Test
    void beforeEnterReroutesAuthenticatedUsersAndLeavesGuestsAlone() {
        // Covers the before-enter reroute branch when a session is authenticated and the no-reroute guest path.
        LoginView view = new LoginView(authService);
        BeforeEnterEvent event = newEvent();

        view.beforeEnter(event);
        assertFalse(event.hasRerouteTarget());

        UiTestSupport.attachSession(UserRole.STUDENT, "Ayesha Khan");
        BeforeEnterEvent authenticatedEvent = newEvent();
        view.beforeEnter(authenticatedEvent);
        assertTrue(authenticatedEvent.hasRerouteTarget());
        assertEquals(DashboardView.class, authenticatedEvent.getRerouteTargetType());
    }

    private BeforeEnterEvent newEvent() {
        UI ui = UiTestSupport.attachUi();
        Router router = mock(Router.class);
        return new BeforeEnterEvent(router, NavigationTrigger.PROGRAMMATIC, new Location("login"), LoginView.class, ui, java.util.List.of());
    }
}