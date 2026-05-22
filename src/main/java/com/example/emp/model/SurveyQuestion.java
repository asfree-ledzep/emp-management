package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class SurveyQuestion {
    private Long questionId;
    private Long surveyId;
    private String questionText;
    private String questionType;  // SINGLE / MULTI / TEXT
    private Integer orderNum;

    // 선택지 (객관식만)
    private List<SurveyOption> options;
}
