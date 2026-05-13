package com.studentportal.repository;

import com.studentportal.model.FeeChallan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeeChallanRepository extends JpaRepository<FeeChallan, Long> {
    Optional<FeeChallan> findTopByStudentIdOrderByIssueDateDesc(Long studentId);
}
