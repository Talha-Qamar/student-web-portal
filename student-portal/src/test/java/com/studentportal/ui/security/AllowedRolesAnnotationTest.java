package com.studentportal.ui.security;

import com.studentportal.model.UserRole;
import com.studentportal.ui.views.AdminAssignmentView;
import com.studentportal.ui.views.AdminDashboardView;
import com.studentportal.ui.views.AdminManageUsersView;
import com.studentportal.ui.views.AttendanceView;
import com.studentportal.ui.views.CoursesView;
import com.studentportal.ui.views.DashboardView;
import com.studentportal.ui.views.EnrollmentsView;
import com.studentportal.ui.views.FacultyAttendanceUploadView;
import com.studentportal.ui.views.FacultyDashboardView;
import com.studentportal.ui.views.FacultyFeedbackInsightsView;
import com.studentportal.ui.views.FacultyGradeUploadView;
import com.studentportal.ui.views.FeeChallanView;
import com.studentportal.ui.views.MarksView;
import com.studentportal.ui.views.StudentFeedbackView;
import com.studentportal.ui.views.TranscriptView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class AllowedRolesAnnotationTest {

    @Test
    void allProtectedViewsDeclareExpectedRoles() {
        assertRoles(DashboardView.class, UserRole.STUDENT);
        assertRoles(CoursesView.class, UserRole.STUDENT);
        assertRoles(EnrollmentsView.class, UserRole.STUDENT);
        assertRoles(AttendanceView.class, UserRole.STUDENT);
        assertRoles(MarksView.class, UserRole.STUDENT);
        assertRoles(TranscriptView.class, UserRole.STUDENT);
        assertRoles(FeeChallanView.class, UserRole.STUDENT);
        assertRoles(StudentFeedbackView.class, UserRole.STUDENT);

        assertRoles(FacultyDashboardView.class, UserRole.FACULTY);
        assertRoles(FacultyFeedbackInsightsView.class, UserRole.FACULTY);
        assertRoles(FacultyGradeUploadView.class, UserRole.FACULTY);
        assertRoles(FacultyAttendanceUploadView.class, UserRole.FACULTY);

        assertRoles(AdminDashboardView.class, UserRole.ADMIN);
        assertRoles(AdminAssignmentView.class, UserRole.ADMIN);
        assertRoles(AdminManageUsersView.class, UserRole.ADMIN);
    }

    private void assertRoles(Class<?> viewClass, UserRole... expectedRoles) {
        AllowedRoles allowedRoles = viewClass.getAnnotation(AllowedRoles.class);
        assertArrayEquals(expectedRoles, allowedRoles.value(), viewClass.getSimpleName());
    }
}