package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LeaveBalance {
    private Integer empno;
    private String  ename;    // 조인용
    private Integer deptno;   // 조인용
    private String  dname;    // 조인용
    private Integer year;
    private Double totalDays;
    private Double usedDays;

    // 계산 필드 (DB에는 없음)
    public Double getRemaining() {
        if (totalDays == null || usedDays == null) return 0.0;
        return totalDays - usedDays;
    }
}
