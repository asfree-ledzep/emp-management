package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Holiday {
    private Long   holidayId;
    private String holidayDate;     // "YYYY-MM-DD" 시작일
    private String holidayEndDate;  // "YYYY-MM-DD" 종료일 (단일 공휴일은 시작일과 동일)
    private String holidayName;
    private String createdBy;
}
