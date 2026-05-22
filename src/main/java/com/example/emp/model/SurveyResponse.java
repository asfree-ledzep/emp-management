package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
public class SurveyResponse {
    private Long responseId;
    private Long surveyId;
    private Integer empno;
    private LocalDateTime submittedAt;

    // 응답 상세 목록
    private List<SurveyAnswer> answers;
}
