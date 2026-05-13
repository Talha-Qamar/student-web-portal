package com.studentportal.service;

import com.studentportal.dto.LoginRequest;
import com.studentportal.dto.LoginResponse;
import com.studentportal.exception.BadRequestException;
import com.studentportal.exception.ResourceNotFoundException;
import com.studentportal.model.AdminUser;
import com.studentportal.model.Faculty;
import com.studentportal.model.Student;
import com.studentportal.model.UserRole;
import com.studentportal.repository.AdminUserRepository;
import com.studentportal.repository.FacultyRepository;
import com.studentportal.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private AdminUserRepository adminUserRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginRejectsBlankRole() {
        LoginRequest request = request("   ", "student@example.com", "secret");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.login(request));
        assertEquals("Invalid role selected", exception.getMessage());
    }

    @Test
    void loginRejectsUnknownRoleValue() {
        LoginRequest request = request("guest", "student@example.com", "secret");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.login(request));
        assertEquals("Invalid role selected", exception.getMessage());
    }

    @Test
    void loginStudentReturnsSessionPayload() {
        Student student = new Student();
        ReflectionTestUtils.setField(student, "id", 11L);
        student.setFullName("Ali Khan");
        student.setEmail("ali@example.com");
        student.setPassword("secret");
        student.setMajor("Computer Science");
        student.setEnrollmentYear(2022);
        student.setGpa(3.5);
        student.setCurrentSemester(4);

        when(studentRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));

        LoginResponse response = authService.login(requestWithRole(UserRole.STUDENT, student.getEmail(), "secret"));

        assertAll(
                () -> assertEquals(UserRole.STUDENT, response.getRole()),
                () -> assertEquals(student.getEmail(), response.getEmail()),
                () -> assertEquals(student.getFullName(), response.getFullName()),
                () -> assertEquals(student.getId(), response.getUserId()),
                () -> assertNotNull(response.getSessionToken()),
                () -> assertEquals(student.getCurrentSemester(), response.getCurrentSemester())
        );
    }

    @Test
    void loginStudentRejectsBadPassword() {
        Student student = new Student();
        student.setPassword("correct");
        when(studentRepository.findByEmail("student@example.com")).thenReturn(Optional.of(student));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> authService.login(requestWithRole(UserRole.STUDENT, "student@example.com", "wrong")));
        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void loginStudentRejectsMissingAccount() {
        when(studentRepository.findByEmail("student@example.com")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> authService.login(requestWithRole(UserRole.STUDENT, "student@example.com", "wrong")));
        assertEquals("Student not found", exception.getMessage());
    }

    @Test
    void loginFacultyAndAdminReturnRoleSpecificPayloads() {
        Faculty faculty = new Faculty();
        ReflectionTestUtils.setField(faculty, "id", 22L);
        faculty.setFullName("Dr. Sara");
        faculty.setEmail("faculty@example.com");
        faculty.setPassword("secret");
        faculty.setDepartment("CS");
        when(facultyRepository.findByEmail(faculty.getEmail())).thenReturn(Optional.of(faculty));

        LoginResponse facultyResponse = authService.login(requestWithRole(UserRole.FACULTY, faculty.getEmail(), "secret"));

        AdminUser admin = new AdminUser();
        ReflectionTestUtils.setField(admin, "id", 33L);
        admin.setFullName("Admin User");
        admin.setEmail("admin@example.com");
        admin.setPassword("secret");
        when(adminUserRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));

        LoginResponse adminResponse = authService.login(requestWithRole(UserRole.ADMIN, admin.getEmail(), "secret"));

        assertAll(
                () -> assertEquals(UserRole.FACULTY, facultyResponse.getRole()),
                () -> assertEquals("CS", facultyResponse.getMajor()),
                () -> assertEquals(UserRole.ADMIN, adminResponse.getRole()),
                () -> assertEquals("Administration", adminResponse.getMajor())
        );
    }

    @Test
    void loginFacultyRejectsBadPasswordAndMissingFaculty() {
        Faculty faculty = new Faculty();
        faculty.setPassword("correct");
        when(facultyRepository.findByEmail("faculty@example.com")).thenReturn(Optional.of(faculty));

        BadRequestException badPassword = assertThrows(BadRequestException.class,
                () -> authService.login(requestWithRole(UserRole.FACULTY, "faculty@example.com", "wrong")));
        assertEquals("Invalid credentials", badPassword.getMessage());

        when(facultyRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        ResourceNotFoundException missingFaculty = assertThrows(ResourceNotFoundException.class,
                () -> authService.login(requestWithRole(UserRole.FACULTY, "missing@example.com", "secret")));
        assertEquals("Faculty user not found", missingFaculty.getMessage());
    }

    @Test
    void loginAdminRejectsBadPasswordAndMissingAccount() {
        AdminUser admin = new AdminUser();
        admin.setPassword("correct");
        when(adminUserRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        BadRequestException badPassword = assertThrows(BadRequestException.class,
                () -> authService.login(requestWithRole(UserRole.ADMIN, "admin@example.com", "wrong")));
        assertEquals("Invalid credentials", badPassword.getMessage());

        when(adminUserRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        ResourceNotFoundException missingAdmin = assertThrows(ResourceNotFoundException.class,
                () -> authService.login(requestWithRole(UserRole.ADMIN, "missing@example.com", "secret")));
        assertEquals("Admin user not found", missingAdmin.getMessage());
    }

    private LoginRequest request(String role, String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setRole(role);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private LoginRequest requestWithRole(UserRole role, String email, String password) {
        return request(role.name(), email, password);
    }
}