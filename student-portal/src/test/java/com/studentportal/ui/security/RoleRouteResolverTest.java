package com.studentportal.ui.security;

import com.studentportal.model.UserRole;
import com.studentportal.ui.views.AdminDashboardView;
import com.studentportal.ui.views.DashboardView;
import com.studentportal.ui.views.FacultyDashboardView;
import com.studentportal.ui.views.RoleSelectionView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleRouteResolverTest {

    @Test
    void resolvesHomeViewForEachRole() {
        assertEquals(RoleSelectionView.class, RoleRouteResolver.homeFor(null));
        assertEquals(DashboardView.class, RoleRouteResolver.homeFor(UserRole.STUDENT));
        assertEquals(FacultyDashboardView.class, RoleRouteResolver.homeFor(UserRole.FACULTY));
        assertEquals(AdminDashboardView.class, RoleRouteResolver.homeFor(UserRole.ADMIN));
    }
}