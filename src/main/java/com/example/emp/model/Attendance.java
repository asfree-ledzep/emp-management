package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Attendance {
    private Long    attendId;
    private Integer empno;
    private String  ename;        // EMP JOIN
    private String  dname;        // DEPT JOIN
    private String  workDate;     // TO_CHAR 'YYYY-MM-DD'
    private String  checkIn;      // TO_CHAR 'HH24:MI'
    private String  checkOut;     // TO_CHAR 'HH24:MI'
    private Integer workMinutes;  // 실 근무 시간 (분)
    private String  status;       // NORMAL / LATE / EARLY_LEAVE / ABSENT
    private String  note;
    private String  createdAt;
}
