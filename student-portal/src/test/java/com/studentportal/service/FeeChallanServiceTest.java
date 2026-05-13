package com.studentportal.service;

import com.studentportal.dto.FeeChallanDto;
import com.studentportal.exception.ResourceNotFoundException;
import com.studentportal.model.Course;
import com.studentportal.model.Enrollment;
import com.studentportal.model.EnrollmentStatus;
import com.studentportal.model.FeeChallan;
import com.studentportal.model.FeeLineItem;
import com.studentportal.model.Student;
import com.studentportal.repository.EnrollmentRepository;
import com.studentportal.repository.FeeChallanRepository;
import com.studentportal.repository.FeeLineItemRepository;
import com.studentportal.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeChallanServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private FeeChallanRepository feeChallanRepository;

    @Mock
    private FeeLineItemRepository feeLineItemRepository;

    @InjectMocks
    private FeeChallanService feeChallanService;

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student();
        ReflectionTestUtils.setField(student, "id", 1L);
        student.setFullName("Ali Khan");
    }

    @Test
    void generateChallanUsesDatabaseRecordWhenPresent() {
        // Covers the database-backed challan path and the line-item mapping branch with a null code.
        FeeChallan challan = new FeeChallan();
        ReflectionTestUtils.setField(challan, "id", 10L);
        challan.setStudent(student);
        challan.setChallanNumber("FAST-0001-20260424");
        challan.setIssueDate(LocalDate.of(2026, 4, 24));
        challan.setDueDate(LocalDate.of(2026, 5, 4));
        challan.setTotalAmount(1200d);
        challan.setTotalCreditHours(null);

        FeeLineItem lineItem = new FeeLineItem();
        lineItem.setCode(null);
        lineItem.setTitle("Lab Fee");
        lineItem.setCreditHours(3);
        lineItem.setAmount(1800d);
        lineItem.setChallan(challan);

        when(feeChallanRepository.findTopByStudentIdOrderByIssueDateDesc(1L)).thenReturn(Optional.of(challan));
        when(feeLineItemRepository.findByChallanIdOrderByIdAsc(10L)).thenReturn(List.of(lineItem));

        FeeChallanDto dto = feeChallanService.generateChallan(1L);

        assertAll(
                () -> assertEquals("FAST-0001-20260424", dto.getChallanNumber()),
                () -> assertEquals(3, dto.getTotalCreditHours()),
            () -> assertEquals(1, dto.getItems().size()),
            () -> assertEquals("-", dto.getItems().get(0).getCode())
        );
    }

    @Test
    void generateChallanBuildsFromActiveEnrollmentsAndAddsSurcharges() {
        // Covers the fallback branch that computes challans from enrollments and the loop that adds fixed fee items.
        Course lab = course(11L, "CS301", true, 3);
        Course theory = course(12L, "CS302", false, 3);
        Enrollment labEnrollment = enrollment(lab, EnrollmentStatus.ENROLLED);
        Enrollment theoryEnrollment = enrollment(theory, EnrollmentStatus.ENROLLED);
        Enrollment dropped = enrollment(course(13L, "CS303", false, 3), EnrollmentStatus.DROPPED);

        when(feeChallanRepository.findTopByStudentIdOrderByIssueDateDesc(1L)).thenReturn(Optional.empty());
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of(labEnrollment, theoryEnrollment, dropped));

        FeeChallanDto dto = feeChallanService.generateChallan(1L);

        assertAll(
                () -> assertTrue(dto.getChallanNumber().startsWith("FAST-0001-")),
                () -> assertEquals(6, dto.getTotalCreditHours()),
                () -> assertEquals(4, dto.getItems().size()),
                () -> assertTrue(dto.getItems().stream().anyMatch(item -> item.getCode().equals("TECH"))),
                () -> assertTrue(dto.getItems().stream().anyMatch(item -> item.getCode().equals("ACT")))
        );
    }

    @Test
    void generateChallanRejectsMissingStudent() {
        // Covers the missing-student exception branch in the computed-challan path.
        when(feeChallanRepository.findTopByStudentIdOrderByIssueDateDesc(99L)).thenReturn(Optional.empty());
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> feeChallanService.generateChallan(99L));
        assertEquals("Student not found", exception.getMessage());
    }

    private Course course(Long id, String code, boolean lab, int credits) {
        Course course = new Course();
        ReflectionTestUtils.setField(course, "id", id);
        course.setCode(code);
        course.setTitle(code + " title");
        course.setLab(lab);
        course.setCreditHours(credits);
        return course;
    }

    private Enrollment enrollment(Course course, EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus(status);
        return enrollment;
    }
}