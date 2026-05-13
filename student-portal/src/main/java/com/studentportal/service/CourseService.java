package com.studentportal.service;

import com.studentportal.exception.ResourceNotFoundException;
import com.studentportal.model.Course;
import com.studentportal.model.Enrollment;
import com.studentportal.model.EnrollmentStatus;
import com.studentportal.model.Student;
import com.studentportal.repository.CourseRepository;
import com.studentportal.repository.EnrollmentRepository;
import com.studentportal.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicRulesService academicRulesService;

    public CourseService(CourseRepository courseRepository,
                         StudentRepository studentRepository,
                         EnrollmentRepository enrollmentRepository,
                         AcademicRulesService academicRulesService) {
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.academicRulesService = academicRulesService;
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Map<Integer, List<Course>> getCatalogBySemester() {
        return getAllCourses().stream()
                .filter(course -> course.getSemesterNumber() != null)
                .collect(Collectors.groupingBy(Course::getSemesterNumber, TreeMap::new, Collectors.toList()));
    }

    public List<Course> getAvailableCoursesForStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        List<Long> alreadyRegistered = enrollments.stream()
                .filter(enrollment -> !EnrollmentStatus.DROPPED.equals(enrollment.getStatus()))
                .filter(enrollment -> enrollment.getCourse() != null)
                .map(enrollment -> enrollment.getCourse().getId())
                .toList();

        LinkedHashMap<Long, Course> shortlist = new LinkedHashMap<>();
        courseRepository.findBySemesterNumberOrderByCode(student.getCurrentSemester())
                .forEach(course -> {
                    if (course.getId() != null) {
                        shortlist.putIfAbsent(course.getId(), course);
                    }
                });

        getAllCourses().stream()
                .filter(course -> course.getSemesterNumber() != null)
                .filter(course -> course.getSemesterNumber() < student.getCurrentSemester())
                .filter(course -> academicRulesService.hasOutstandingRepeat(student, course))
                .forEach(course -> {
                    if (course.getId() != null) {
                        shortlist.putIfAbsent(course.getId(), course);
                    }
                });

        return shortlist.values().stream()
                .filter(course -> !alreadyRegistered.contains(course.getId()))
                .collect(Collectors.toList());
    }

    public Course getCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }
}
