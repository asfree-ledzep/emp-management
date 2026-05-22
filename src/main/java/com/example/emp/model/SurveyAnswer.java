package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SurveyAnswer {
    private Long answerId;
    private Long responseId;
    private Long questionId;
    private Long optionId;      // 객관식
    private String textValue;   // 주관식
}
