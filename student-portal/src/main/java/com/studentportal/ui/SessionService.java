package com.studentportal.ui;

import com.studentportal.dto.LoginResponse;
import com.studentportal.model.UserRole;
import com.vaadin.flow.server.VaadinSession;

import java.util.Optional;

/**
 * Simple helper around {@link VaadinSession} for storing/retrieving the logged-in student context.
 */
public final class SessionService {

    private static final String USER_ID_KEY = "user-id";
    private static final String ROLE_KEY = "user-role";
    private static final String STUDENT_ID_KEY = "student-id";
    private static final String STUDENT_NAME_KEY = "student-name";
    private static final String STUDENT_EMAIL_KEY = "student-email";
    private static final String STUDENT_MAJOR_KEY = "student-major";
    private static final String STUDENT_SEMESTER_KEY = "student-semester";

    private SessionService() {
    }

    public static void store(LoginResponse response) {
        VaadinSession session = requireSession();
        session.setAttribute(USER_ID_KEY, response.getUserId());
        session.setAttribute(ROLE_KEY, response.getRole() != null ? response.getRole().name() : null);
        session.setAttribute(STUDENT_ID_KEY, response.getStudentId());
        session.setAttribute(STUDENT_NAME_KEY, response.getFullName());
        session.setAttribute(STUDENT_EMAIL_KEY, response.getEmail());
        session.setAttribute(STUDENT_MAJOR_KEY, response.getMajor());
        session.setAttribute(STUDENT_SEMESTER_KEY, response.getCurrentSemester());
    }

    public static Optional<Long> getUserId() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return Optional.empty();
        }
        Object value = session.getAttribute(USER_ID_KEY);
        if (value instanceof Long longValue) {
            return Optional.of(longValue);
        }
        if (value instanceof Number number) {
            return Optional.of(number.longValue());
        }
        return Optional.empty();
    }

    public static Optional<UserRole> getRole() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return Optional.empty();
        }
        Object value = session.getAttribute(ROLE_KEY);
        if (value instanceof String roleName && !roleName.isBlank()) {
            try {
                return Optional.of(UserRole.valueOf(roleName));
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static boolean hasRole(UserRole role) {
        return getRole().map(role::equals).orElse(false);
    }

    public static Long requireUserId() {
        return getUserId().orElseThrow(() -> new IllegalStateException("No authenticated user in session"));
    }

    public static Optional<Long> getStudentId() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return Optional.empty();
        }
        Object value = session.getAttribute(STUDENT_ID_KEY);
        if (value instanceof Long longValue) {
            return Optional.of(longValue);
        }
        if (value instanceof Number number) {
            return Optional.of(number.longValue());
        }
        return Optional.empty();
    }

    public static Long requireStudentId() {
        if (!hasRole(UserRole.STUDENT)) {
            throw new IllegalStateException("Current session is not a student account");
        }
        return getStudentId().orElseThrow(() -> new IllegalStateException("No authenticated student in session"));
    }

    public static Optional<String> getStudentName() {
        return getAttribute(STUDENT_NAME_KEY);
    }

    public static Optional<String> getStudentEmail() {
        return getAttribute(STUDENT_EMAIL_KEY);
    }

    public static Optional<String> getStudentMajor() {
        return getAttribute(STUDENT_MAJOR_KEY);
    }

    public static Optional<Integer> getCurrentSemester() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return Optional.empty();
        }
        Object value = session.getAttribute(STUDENT_SEMESTER_KEY);
        if (value instanceof Number number) {
            return Optional.of(number.intValue());
        }
        return Optional.empty();
    }

    public static boolean isAuthenticated() {
        return getUserId().isPresent();
    }

    public static void clear() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return;
        }
        session.setAttribute(USER_ID_KEY, null);
        session.setAttribute(ROLE_KEY, null);
        session.setAttribute(STUDENT_ID_KEY, null);
        session.setAttribute(STUDENT_NAME_KEY, null);
        session.setAttribute(STUDENT_EMAIL_KEY, null);
        session.setAttribute(STUDENT_MAJOR_KEY, null);
        session.setAttribute(STUDENT_SEMESTER_KEY, null);
        session.close();
    }

    private static Optional<String> getAttribute(String key) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return Optional.empty();
        }
        Object value = session.getAttribute(key);
        return value != null ? Optional.of(value.toString()) : Optional.empty();
    }

    private static VaadinSession requireSession() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            throw new IllegalStateException("No active Vaadin session");
        }
        return session;
    }
}
