package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class ExpenseYearlyStat {
    private Long statId;
    private Integer statYear;
    private Integer empno;
    private String ename;
    private Double totalAmount;
    private Integer expenseCount;
    private LocalDateTime createdAt;
}
