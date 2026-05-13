package com.studentportal.ui.security;

import com.studentportal.model.UserRole;
import com.studentportal.ui.views.AdminDashboardView;
import com.studentportal.ui.views.DashboardView;
import com.studentportal.ui.views.FacultyDashboardView;
import com.studentportal.ui.views.RoleSelectionView;
import com.vaadin.flow.component.Component;

public final class RoleRouteResolver {

    private RoleRouteResolver() {
    }

    public static Class<? extends Component> homeFor(UserRole role) {
        if (role == null) {
            return RoleSelectionView.class;
        }
        return switch (role) {
            case STUDENT -> DashboardView.class;
            case FACULTY -> FacultyDashboardView.class;
            case ADMIN -> AdminDashboardView.class;
        };
    }
}
