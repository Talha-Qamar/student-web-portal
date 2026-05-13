package com.studentportal.ui;

import com.studentportal.dto.LoginResponse;
import com.studentportal.model.UserRole;
import com.vaadin.flow.server.PwaRegistry;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionServiceTest {

    private VaadinSession session;

    @AfterEach
    void tearDown() {
        VaadinSession.setCurrent(null);
    }

    @Test
    void storePersistsStudentContextAndSupportsReadAccess() {
        session = createAttachedSession();

        LoginResponse response = new LoginResponse(
                42L,
                UserRole.STUDENT,
                1001L,
                "Ayesha Khan",
                "ayesha@example.com",
                "Computer Science",
                2023,
                3.71,
                "token-123",
                5
        );

        SessionService.store(response);

        assertAll(
                () -> assertEquals(42L, SessionService.requireUserId()),
                () -> assertEquals(UserRole.STUDENT, SessionService.getRole().orElseThrow()),
                () -> assertTrue(SessionService.hasRole(UserRole.STUDENT)),
                () -> assertEquals(1001L, SessionService.requireStudentId()),
                () -> assertEquals("Ayesha Khan", SessionService.getStudentName().orElseThrow()),
                () -> assertEquals("ayesha@example.com", SessionService.getStudentEmail().orElseThrow()),
                () -> assertEquals("Computer Science", SessionService.getStudentMajor().orElseThrow()),
                () -> assertEquals(5, SessionService.getCurrentSemester().orElseThrow()),
                () -> assertTrue(SessionService.isAuthenticated())
        );
    }

    @Test
    void gettersReturnEmptyWithoutCurrentSession() {
        VaadinSession.setCurrent(null);

        assertAll(
                () -> assertFalse(SessionService.getUserId().isPresent()),
                () -> assertFalse(SessionService.getRole().isPresent()),
                () -> assertFalse(SessionService.getStudentId().isPresent()),
                () -> assertFalse(SessionService.getStudentName().isPresent()),
                () -> assertFalse(SessionService.getStudentEmail().isPresent()),
                () -> assertFalse(SessionService.getStudentMajor().isPresent()),
                () -> assertFalse(SessionService.getCurrentSemester().isPresent()),
                () -> assertFalse(SessionService.isAuthenticated())
        );
    }

    @Test
    void numericSessionValuesAreConvertedAndInvalidRoleStringsAreIgnored() {
        session = createAttachedSession();
        session.setAttribute("user-id", Integer.valueOf(17));
        session.setAttribute("student-id", Short.valueOf((short) 301));
        session.setAttribute("student-semester", Double.valueOf(6.0));
        session.setAttribute("user-role", "INVALID_ROLE");

        assertAll(
                () -> assertEquals(17L, SessionService.getUserId().orElseThrow()),
                () -> assertEquals(301L, SessionService.getStudentId().orElseThrow()),
                () -> assertEquals(6, SessionService.getCurrentSemester().orElseThrow()),
                () -> assertFalse(SessionService.getRole().isPresent()),
                () -> assertTrue(SessionService.isAuthenticated())
        );
    }

    @Test
    void blankRoleStringsAreIgnored() {
        session = createAttachedSession();
        session.setAttribute("user-role", "   ");

        assertFalse(SessionService.getRole().isPresent());
    }

    @Test
    void requireStudentIdRejectsNonStudentRoles() {
        session = createAttachedSession();
        session.setAttribute("user-role", UserRole.FACULTY.name());
        session.setAttribute("student-id", 777L);

        IllegalStateException exception = assertThrows(IllegalStateException.class, SessionService::requireStudentId);
        assertEquals("Current session is not a student account", exception.getMessage());
    }

    @Test
    void requireStudentIdRejectsMissingStudentIdForStudentRole() {
        session = createAttachedSession();
        session.setAttribute("user-role", UserRole.STUDENT.name());

        IllegalStateException exception = assertThrows(IllegalStateException.class, SessionService::requireStudentId);
        assertEquals("No authenticated student in session", exception.getMessage());
    }

    @Test
    void clearRemovesStoredValuesAndHandlesMissingSession() {
        SessionService.clear();

        session = createAttachedSession();
        LoginResponse response = new LoginResponse(
                55L,
                UserRole.STUDENT,
                2201L,
                "Test Student",
                "test@student.edu",
                "Software Engineering",
                2024,
                3.25,
                "token-456",
                3
        );

        SessionService.store(response);
        SessionService.clear();

        assertAll(
                () -> assertFalse(SessionService.getUserId().isPresent()),
                () -> assertFalse(SessionService.getRole().isPresent()),
                () -> assertFalse(SessionService.getStudentId().isPresent()),
                () -> assertFalse(SessionService.getStudentName().isPresent()),
                () -> assertFalse(SessionService.getStudentEmail().isPresent()),
                () -> assertFalse(SessionService.getStudentMajor().isPresent()),
                () -> assertFalse(SessionService.getCurrentSemester().isPresent())
        );
    }

    @Test
    void storeFailsFastWithoutAnActiveSession() {
        VaadinSession.setCurrent(null);

        LoginResponse response = new LoginResponse(
                99L,
                UserRole.STUDENT,
                9001L,
                "Detached User",
                "detached@example.com",
                "Information Technology",
                2022,
                2.9,
                "token-789",
                7
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> SessionService.store(response));
        assertEquals("No active Vaadin session", exception.getMessage());
    }

    private VaadinSession createAttachedSession() {
        VaadinService service = new TestVaadinService();
        VaadinSession attachedSession = new TestVaadinSession(service);
        VaadinSession.setCurrent(attachedSession);
        return attachedSession;
    }

    private static final class TestVaadinSession extends VaadinSession {

        private TestVaadinSession(VaadinService service) {
            super(service);
        }

        @Override
        public void checkHasLock(String message) {
        }

        @Override
        public void checkHasLock() {
        }

        @Override
        public boolean hasLock() {
            return true;
        }
    }

    private static final class TestVaadinService extends VaadinService {

        private TestVaadinService() {
            super();
        }

        @Override
        protected RouteRegistry getRouteRegistry() {
            return null;
        }

        @Override
        protected PwaRegistry getPwaRegistry() {
            return null;
        }

        @Override
        public String getContextRootRelativePath(VaadinRequest request) {
            return "/";
        }

        @Override
        public String getMimeType(String resource) {
            return "text/plain";
        }

        @Override
        public String getServiceName() {
            return "test-service";
        }

        @Override
        public String getMainDivId(VaadinSession session, VaadinRequest request) {
            return "root";
        }

        @Override
        public java.net.URL getStaticResource(String path) {
            return null;
        }

        @Override
        public java.net.URL getResource(String path) {
            return null;
        }

        @Override
        public java.io.InputStream getResourceAsStream(String path) {
            return null;
        }

        @Override
        public String resolveResource(String path) {
            return path;
        }

        @Override
        protected boolean requestCanCreateSession(VaadinRequest request) {
            return false;
        }

        @Override
        protected VaadinContext constructVaadinContext() {
            return null;
        }
    }
}