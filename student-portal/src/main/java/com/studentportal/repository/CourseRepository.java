package com.studentportal.repository;

import com.studentportal.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCode(String code);
    List<Course> findBySemesterNumberOrderByCode(Integer semesterNumber);
    List<Course> findBySemesterNumberInOrderBySemesterNumberAsc(Collection<Integer> semesterNumbers);
}
