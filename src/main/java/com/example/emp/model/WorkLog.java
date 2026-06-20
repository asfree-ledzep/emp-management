package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
public class WorkLog {
    private Long    logId;
    private Integer empno;
    private String  ename;        // 조인용
    private String  deptName;     // 조인용
    private String  job;          // 조인용
    private LocalDate logDate;
    private String  title;
    private String  content;
    // DRAFT / SUBMITTED / MGR_APPROVED / APPROVED / MGR_REJECTED / ADMIN_REJECTED
    private String  status;
    private Integer mgrEmpno;
    private String  mgrEname;     // 조인용
    private String  mgrComment;
    private LocalDateTime mgrAt;
    private String  adminComment;
    private LocalDateTime adminAt;
    private String  rejectReason;
    private String  rejectedBy;   // MGR or ADMIN
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
