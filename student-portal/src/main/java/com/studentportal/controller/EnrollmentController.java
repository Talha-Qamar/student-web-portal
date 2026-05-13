package com.studentportal.controller;

import com.studentportal.dto.DropCourseRequest;
import com.studentportal.dto.RegisterCourseRequest;
import com.studentportal.model.Enrollment;
import com.studentportal.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<List<Enrollment>> list(@PathVariable("studentId") Long studentId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsForStudent(studentId));
    }

    @PostMapping("/register")
    public ResponseEntity<Enrollment> register(@Valid @RequestBody RegisterCourseRequest request) {
        return ResponseEntity.ok(enrollmentService.registerCourse(request));
    }

    @PostMapping("/drop")
    public ResponseEntity<Void> drop(@Valid @RequestBody DropCourseRequest request) {
        enrollmentService.dropCourse(request);
        return ResponseEntity.noContent().build();
    }
}
