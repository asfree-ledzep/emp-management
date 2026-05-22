package com.example.emp.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class OcrResultDto {
    private String receiptUrl;
    private Double amount;
    private String expenseDate;   // "yyyy-MM-dd"
    private String ocrRaw;
}
