package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Notice {
    private Long noticeId;
    private String title;
    private String content;
    private String createdBy;
    private LocalDateTime createdAt;
}
