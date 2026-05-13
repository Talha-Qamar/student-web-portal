package com.studentportal.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.studentportal.dto.SemesterTranscriptBlock;
import com.studentportal.dto.TranscriptCourseDto;
import com.studentportal.dto.TranscriptResponse;
import com.studentportal.exception.ResourceNotFoundException;
import com.studentportal.model.Course;
import com.studentportal.model.Enrollment;
import com.studentportal.model.EnrollmentStatus;
import com.studentportal.model.SemesterProgress;
import com.studentportal.model.Student;
import com.studentportal.repository.EnrollmentRepository;
import com.studentportal.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class TranscriptService {

    private static final Map<String, Double> GRADE_POINTS = new LinkedHashMap<>() {{
        put("A", 4.0);
        put("A-", 3.7);
        put("B+", 3.4);
        put("B", 3.0);
        put("B-", 2.7);
        put("C+", 2.4);
        put("C", 2.0);
        put("C-", 1.7);
        put("D", 1.0);
        put("F", 0.0);
    }};

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicRulesService academicRulesService;

    public TranscriptService(StudentRepository studentRepository,
                             EnrollmentRepository enrollmentRepository,
                             AcademicRulesService academicRulesService) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.academicRulesService = academicRulesService;
    }

    public TranscriptResponse getTranscript(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        academicRulesService.refreshRepeatFlags(studentId);
        academicRulesService.ensureSemesterProgress(student);
        academicRulesService.updateSummaries(studentId);

        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        Map<Integer, List<Enrollment>> bySemester = enrollments.stream()
                .filter(enrollment -> enrollment.getCourse() != null)
                .filter(enrollment -> enrollment.getCourse().getSemesterNumber() != null)
                .collect(Collectors.groupingBy(enrollment -> enrollment.getCourse().getSemesterNumber(), TreeMap::new, Collectors.toList()));

        Map<Integer, SemesterProgress> progressMap = academicRulesService.progressBySemester(studentId);

        List<TranscriptCourseDto> flattenedCourses = new ArrayList<>();
        List<SemesterTranscriptBlock> blocks = new ArrayList<>();
        double cgpaPoints = 0.0;
        int cgpaCredits = 0;

        for (int semester = 1; semester <= 8; semester++) {
            List<Enrollment> semesterEnrollments = bySemester.getOrDefault(semester, List.of());
            SemesterProgress progress = progressMap.get(semester);
            boolean finalized = progress != null && progress.isFinalized();

            List<TranscriptCourseDto> courseDtos = new ArrayList<>();
            for (Enrollment enrollment : semesterEnrollments) {
                if (EnrollmentStatus.DROPPED.equals(enrollment.getStatus())) {
                    continue;
                }
                Course course = enrollment.getCourse();
                Integer credits = course.getCreditHours();
                Double points = gradePoints(enrollment.getGrade());
                if (finalized && credits != null && points != null) {
                    cgpaPoints += points * credits;
                    cgpaCredits += credits;
                }
                TranscriptCourseDto dto = new TranscriptCourseDto(
                        course.getCode(),
                        course.getTitle(),
                        course.getTerm(),
                        credits,
                        enrollment.getGrade(),
                        enrollment.getStatus() != null ? enrollment.getStatus().name() : EnrollmentStatus.ENROLLED.name(),
                        points,
                        semester,
                        enrollment.isRepeatRequired()
                );
                courseDtos.add(dto);
            }

            flattenedCourses.addAll(courseDtos);

                double semesterGpa = progress != null && progress.getSemesterGpa() != null
                    ? progress.getSemesterGpa()
                    : calculateSemesterGpa(semesterEnrollments);

                int earnedCredits = progress != null && progress.getEarnedCredits() != null
                    ? progress.getEarnedCredits()
                    : calculateEarnedCredits(semesterEnrollments);

            blocks.add(new SemesterTranscriptBlock(
                    semester,
                    finalized,
                    semesterGpa,
                    earnedCredits,
                    courseDtos
            ));
        }

        Double overallGpa = cgpaCredits > 0 ? cgpaPoints / cgpaCredits : student.getGpa();

        return new TranscriptResponse(
                student.getFullName(),
                student.getMajor(),
                student.getEnrollmentYear(),
                roundToTwo(overallGpa != null ? overallGpa : 0.0),
                cgpaCredits,
                flattenedCourses,
                blocks
        );
    }

    public byte[] exportTranscriptPdf(Long studentId) {
        TranscriptResponse transcript = getTranscript(studentId);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            document.add(new Paragraph("FAST Student Transcript", titleFont));
            document.add(new Paragraph("Name: " + transcript.getStudentName(), textFont));
            document.add(new Paragraph("Major: " + transcript.getMajor(), textFont));
            document.add(new Paragraph("Enrollment Year: " + transcript.getEnrollmentYear(), textFont));
            document.add(new Paragraph("CGPA: " + formatNumber(transcript.getOverallGpa()), textFont));
            document.add(new Paragraph("Generated on: " + DateTimeFormatter.ISO_DATE.format(java.time.LocalDate.now()), textFont));
            document.add(new Paragraph(" "));

            for (SemesterTranscriptBlock block : transcript.getSemesters()) {
                document.add(new Paragraph("Semester " + block.getSemesterNumber()
                        + (block.isFinalized() ? " (Finalized)" : " (In progress)"), subTitleFont));
                document.add(new Paragraph("Semester GPA: " + formatNumber(block.getSemesterGpa())
                        + " | Credits: " + block.getCreditsEarned(), textFont));

                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                addHeaderCell(table, "Code");
                addHeaderCell(table, "Title");
                addHeaderCell(table, "Credits");
                addHeaderCell(table, "Grade");
                addHeaderCell(table, "Status");

                for (TranscriptCourseDto dto : block.getCourses()) {
                    table.addCell(new Phrase(dto.getCode(), textFont));
                    table.addCell(new Phrase(dto.getTitle(), textFont));
                    table.addCell(new Phrase(String.valueOf(dto.getCreditHours()), textFont));
                    table.addCell(new Phrase(dto.getGrade() != null ? dto.getGrade() : "-", textFont));
                    String status = dto.isRepeatRequired() ? "Repeat" : dto.getStatus();
                    table.addCell(new Phrase(status, textFont));
                }
                document.add(table);
                document.add(new Paragraph(" "));
            }

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Unable to generate PDF", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Unexpected error creating transcript", ex);
        }
    }

    private Double gradePoints(String grade) {
        if (grade == null) {
            return null;
        }
        return GRADE_POINTS.getOrDefault(grade.toUpperCase(), null);
    }

    private double calculateSemesterGpa(List<Enrollment> enrollments) {
        double totalPoints = 0.0;
        int credits = 0;
        for (Enrollment enrollment : enrollments) {
            Integer ch = enrollment.getCourse() != null ? enrollment.getCourse().getCreditHours() : null;
            if (ch == null) {
                continue;
            }
            Double points = gradePoints(enrollment.getGrade());
            if (points == null) {
                continue;
            }
            totalPoints += points * ch;
            credits += ch;
        }
        if (credits == 0) {
            return 0.0;
        }
        return Math.round((totalPoints / credits) * 100.0) / 100.0;
    }

    private int calculateEarnedCredits(List<Enrollment> enrollments) {
        return enrollments.stream()
                .filter(enrollment -> EnrollmentStatus.COMPLETED.equals(enrollment.getStatus())
                        || enrollment.getGrade() != null)
                .map(Enrollment::getCourse)
                .filter(course -> course != null && course.getCreditHours() != null)
                .mapToInt(course -> course.getCreditHours())
                .sum();
    }

    private Double roundToTwo(Double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void addHeaderCell(PdfPTable table, String label) {
        PdfPCell cell = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        cell.setBackgroundColor(new Color(230, 235, 245));
        table.addCell(cell);
    }

    private String formatNumber(Double value) {
        return value == null ? "0.00" : String.format("%.2f", value);
    }
}
