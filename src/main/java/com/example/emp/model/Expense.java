package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
public class Expense {
    private Long expenseId;
    private Integer empno;
    private String ename;          // EMP 조인용
    private String receiptUrl;
    private LocalDate expenseDate;
    private Double amount;
    private String description;
    private String category;       // 식비/교통비/숙박비/기타
    private String status;         // PENDING / CONFIRMED
    private String ocrRaw;
    private String ocrHash;         // OCR 원문 SHA-256 해시 (중복 제출 방지)
    private LocalDateTime createdAt;
    private String confirmedBy;
    private LocalDateTime confirmedAt;
}
