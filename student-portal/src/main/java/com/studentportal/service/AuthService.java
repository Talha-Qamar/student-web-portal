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
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final AdminUserRepository adminUserRepository;

    public AuthService(StudentRepository studentRepository,
                       FacultyRepository facultyRepository,
                       AdminUserRepository adminUserRepository) {
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.adminUserRepository = adminUserRepository;
    }

    public LoginResponse login(LoginRequest request) {
        UserRole role;
        try {
            role = UserRole.fromValue(request.getRole());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid role selected");
        }

        return switch (role) {
            case STUDENT -> loginStudent(request);
            case FACULTY -> loginFaculty(request);
            case ADMIN -> loginAdmin(request);
        };
    }

    private LoginResponse loginStudent(LoginRequest request) {
        Student student = studentRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (!student.getPassword().equals(request.getPassword())) {
            throw new BadRequestException("Invalid credentials");
        }

        String token = UUID.randomUUID().toString();
        return new LoginResponse(
                student.getId(),
                UserRole.STUDENT,
                student.getId(),
                student.getFullName(),
                student.getEmail(),
                student.getMajor(),
                student.getEnrollmentYear(),
                student.getGpa(),
                token,
                student.getCurrentSemester()
        );
    }

    private LoginResponse loginFaculty(LoginRequest request) {
        Faculty faculty = facultyRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Faculty user not found"));
        if (!faculty.getPassword().equals(request.getPassword())) {
            throw new BadRequestException("Invalid credentials");
        }
        return new LoginResponse(
                faculty.getId(),
                UserRole.FACULTY,
                null,
                faculty.getFullName(),
                faculty.getEmail(),
                faculty.getDepartment(),
                null,
                null,
                UUID.randomUUID().toString(),
                null
        );
    }

    private LoginResponse loginAdmin(LoginRequest request) {
        AdminUser admin = adminUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));
        if (!admin.getPassword().equals(request.getPassword())) {
            throw new BadRequestException("Invalid credentials");
        }
        return new LoginResponse(
                admin.getId(),
                UserRole.ADMIN,
                null,
                admin.getFullName(),
                admin.getEmail(),
                "Administration",
                null,
                null,
                UUID.randomUUID().toString(),
                null
        );
    }
}
