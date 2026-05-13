package com.studentportal.service;

import com.studentportal.dto.FeeChallanDto;
import com.studentportal.dto.FeeLineItemDto;
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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class FeeChallanService {

    private static final double TUITION_PER_CREDIT = 6200d;
    private static final double LAB_SURCHARGE = 1800d;
    private static final double TECHNOLOGY_FEE = 1200d;
    private static final double STUDENT_ACTIVITY_FEE = 1500d;

    private final StudentRepository studentRepository;
        private final EnrollmentRepository enrollmentRepository;
        private final FeeChallanRepository feeChallanRepository;
        private final FeeLineItemRepository feeLineItemRepository;

    public FeeChallanService(StudentRepository studentRepository,
                     EnrollmentRepository enrollmentRepository,
                     FeeChallanRepository feeChallanRepository,
                     FeeLineItemRepository feeLineItemRepository) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.feeChallanRepository = feeChallanRepository;
        this.feeLineItemRepository = feeLineItemRepository;
    }

    public FeeChallanDto generateChallan(Long studentId) {
        return feeChallanRepository.findTopByStudentIdOrderByIssueDateDesc(studentId)
            .map(this::buildChallanFromDatabase)
            .orElseGet(() -> buildChallanFromEnrollments(studentId));
        }

        private FeeChallanDto buildChallanFromDatabase(FeeChallan challan) {
        List<FeeLineItemDto> items = feeLineItemRepository.findByChallanIdOrderByIdAsc(challan.getId()).stream()
            .map(this::mapLineItem)
            .toList();

        int credits = challan.getTotalCreditHours() != null
            ? challan.getTotalCreditHours()
            : items.stream().mapToInt(FeeLineItemDto::getCreditHours).sum();

        return new FeeChallanDto(
            challan.getChallanNumber(),
            challan.getStudent() != null ? challan.getStudent().getFullName() : "Student",
            challan.getIssueDate(),
            challan.getDueDate(),
            items,
            safeAmount(challan.getTotalAmount()),
            credits
        );
        }

        private FeeLineItemDto mapLineItem(FeeLineItem lineItem) {
        return new FeeLineItemDto(
            lineItem.getCode() != null ? lineItem.getCode() : "-",
            lineItem.getTitle(),
            lineItem.getCreditHours() != null ? lineItem.getCreditHours() : 0,
            safeAmount(lineItem.getAmount())
        );
        }

        private FeeChallanDto buildChallanFromEnrollments(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        List<Enrollment> activeEnrollments = enrollmentRepository.findByStudentId(studentId).stream()
                .filter(enrollment -> EnrollmentStatus.ENROLLED.equals(enrollment.getStatus()))
                .filter(enrollment -> enrollment.getCourse() != null)
                .sorted(Comparator.comparing(enrollment -> enrollment.getCourse().getCode()))
                .toList();

        List<FeeLineItemDto> items = new ArrayList<>();
        int totalCreditHours = 0;
        double tuitionSubtotal = 0d;

        for (Enrollment enrollment : activeEnrollments) {
            Course course = enrollment.getCourse();
            int creditHours = course.getCreditHours() != null ? course.getCreditHours() : 3;
            double amount = creditHours * TUITION_PER_CREDIT;
            if (course.isLab()) {
                amount += LAB_SURCHARGE;
            }
            amount = round(amount);
            totalCreditHours += creditHours;
            tuitionSubtotal += amount;
            items.add(new FeeLineItemDto(course.getCode(), course.getTitle(), creditHours, amount));
        }

        items.add(new FeeLineItemDto("TECH", "Technology services bundle", 0, TECHNOLOGY_FEE));
        items.add(new FeeLineItemDto("ACT", "Student activity fund", 0, STUDENT_ACTIVITY_FEE));

        double totalAmount = round(tuitionSubtotal + TECHNOLOGY_FEE + STUDENT_ACTIVITY_FEE);

        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(10);
        String challanNumber = String.format("FAST-%04d-%s",
                student.getId(),
                DateTimeFormatter.BASIC_ISO_DATE.format(issueDate));

        return new FeeChallanDto(
                challanNumber,
                student.getFullName(),
                issueDate,
                dueDate,
                items,
                totalAmount,
                totalCreditHours
        );
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double safeAmount(Double value) {
        return value == null ? 0d : value;
    }
}
