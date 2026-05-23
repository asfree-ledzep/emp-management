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

    // 관리자 응답: 읽은 사원 수
    private Integer readCount;

    // 사원 응답: 본인 읽음 여부
    private Boolean isRead;
}
