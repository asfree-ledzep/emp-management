package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Holiday {
    private Long   holidayId;
    private String holidayDate;  // "YYYY-MM-DD"
    private String holidayName;
    private String createdBy;
}
