package com.studentportal.dto;

import java.time.LocalDate;
import java.util.List;

public class FeeChallanDto {

    private final String challanNumber;
    private final String studentName;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private final List<FeeLineItemDto> items;
    private final double totalAmount;
    private final int totalCreditHours;

    public FeeChallanDto(String challanNumber,
                         String studentName,
                         LocalDate issueDate,
                         LocalDate dueDate,
                         List<FeeLineItemDto> items,
                         double totalAmount,
                         int totalCreditHours) {
        this.challanNumber = challanNumber;
        this.studentName = studentName;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.items = items;
        this.totalAmount = totalAmount;
        this.totalCreditHours = totalCreditHours;
    }

    public String getChallanNumber() {
        return challanNumber;
    }

    public String getStudentName() {
        return studentName;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public List<FeeLineItemDto> getItems() {
        return items;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public int getTotalCreditHours() {
        return totalCreditHours;
    }
}
