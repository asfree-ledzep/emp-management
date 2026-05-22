package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
public class Survey {
    private Long surveyId;
    private String title;
    private String description;
    private String status;       // ACTIVE / CLOSED
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDate endDate;

    // 조회 시 문항 포함 (중첩)
    private List<SurveyQuestion> questions;
    // 응답 현황
    private Integer responseCount;
}
