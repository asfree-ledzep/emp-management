package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
public class LeaveRequest {
    private Long leaveId;
    private Integer empno;
    private String ename;           // 조인용
    private String deptName;        // 조인용
    private String leaveType;       // 연차, 반차, 병가, 특별휴가
    private LocalDate startDate;
    private LocalDate endDate;
    private Double days;
    private String reason;
    // PENDING → MGR_APPROVED / MGR_REJECTED → APPROVED / REJECTED
    private String status;
    private Integer mgrEmpno;
    private String mgrEname;        // 조인용
    private String mgrComment;
    private LocalDateTime mgrAt;
    private String adminComment;
    private LocalDateTime adminAt;
    private LocalDateTime createdAt;
}
