package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SurveyResultDto {
    private Long questionId;
    private String questionText;
    private String questionType;
    private Long optionId;
    private String optionText;
    private Long answerCount;
}
