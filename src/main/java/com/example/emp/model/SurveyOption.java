package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SurveyOption {
    private Long optionId;
    private Long questionId;
    private String optionText;
    private Integer orderNum;
}
