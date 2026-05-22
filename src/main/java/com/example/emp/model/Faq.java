package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Faq {
    private Long faqId;
    private String category;
    private String question;
    private String keywords;   // 콤마 구분 키워드
    private String answer;
    private String createdBy;
    private LocalDateTime createdAt;
}
