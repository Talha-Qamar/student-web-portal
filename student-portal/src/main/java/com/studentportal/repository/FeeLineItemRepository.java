package com.studentportal.repository;

import com.studentportal.model.FeeLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeLineItemRepository extends JpaRepository<FeeLineItem, Long> {
    List<FeeLineItem> findByChallanIdOrderByIdAsc(Long challanId);
}
