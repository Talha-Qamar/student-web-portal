package com.studentportal.dto;

public class FeeLineItemDto {

    private final String code;
    private final String title;
    private final int creditHours;
    private final double amount;

    public FeeLineItemDto(String code, String title, int creditHours, double amount) {
        this.code = code;
        this.title = title;
        this.creditHours = creditHours;
        this.amount = amount;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public int getCreditHours() {
        return creditHours;
    }

    public double getAmount() {
        return amount;
    }
}
