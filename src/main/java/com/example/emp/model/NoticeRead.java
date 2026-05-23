package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NoticeRead {
    private Long          noticeId;
    private Integer       empno;
    private String        ename;
    private LocalDateTime readAt;
}
