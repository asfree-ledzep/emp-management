package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NoticeReadSummaryDto {
    private int readCount;   // 읽은 사원 수
    private int totalEmp;    // 전체 사원 수
    private List<NoticeRead> readers;    // 읽은 사원 목록
    private List<NoticeRead> unreaders;  // 안 읽은 사원 목록
}
