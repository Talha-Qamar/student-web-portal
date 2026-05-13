package com.studentportal.ui;

import com.studentportal.model.UserRole;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.PwaRegistry;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;
import org.mockito.Mockito;

public final class UiTestSupport {

    private UiTestSupport() {
    }

    public static VaadinSession attachSession(UserRole role, String studentName) {
        TestVaadinSession session = new TestVaadinSession(new TestVaadinService());
        VaadinService.setCurrent(session.getService());
        if (role != null) {
            session.setAttribute("user-id", 1L);
            session.setAttribute("user-role", role.name());
        }
        if (studentName != null) {
            session.setAttribute("student-name", studentName);
        }
        VaadinSession.setCurrent(session);
        return session;
    }

    public static UI attachUi() {
        if (VaadinSession.getCurrent() == null) {
            attachSession(null, null);
        }
        UI ui = new UI();
        VaadinSession currentSession = VaadinSession.getCurrent();
        if (currentSession != null) {
            ui.getInternals().setSession(currentSession);
        }
        UI.setCurrent(ui);
        return ui;
    }

    public static void clear() {
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
        VaadinService.setCurrent(null);
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

        private final RouteRegistry routeRegistry = createRouteRegistry();

        private TestVaadinService() {
            super();
        }

        @Override
        protected RouteRegistry getRouteRegistry() {
            return routeRegistry;
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

        private RouteRegistry createRouteRegistry() {
            return Mockito.mock(RouteRegistry.class);
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