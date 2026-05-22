package com.example.emp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ExpenseScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpenseScheduler.class);

    @Autowired
    private ExpenseService expenseService;

    // 매월 말일 01:00 - 월별 통계 자동 생성
    @Scheduled(cron = "0 0 1 L * ?")
    public void generateMonthlyStats() {
        LocalDate today = LocalDate.now();
        int year  = today.getYear();
        int month = today.getMonthValue();
        log.info("[ExpenseScheduler] 월별 통계 생성 시작: {}년 {}월", year, month);
        try {
            expenseService.generateMonthlyStats(year, month);
            log.info("[ExpenseScheduler] 월별 통계 생성 완료");
        } catch (Exception e) {
            log.error("[ExpenseScheduler] 월별 통계 생성 실패: {}", e.getMessage());
        }
    }

    // 매년 12월 31일 02:00 - 연별 통계 자동 생성
    @Scheduled(cron = "0 0 2 31 12 ?")
    public void generateYearlyStats() {
        int year = LocalDate.now().getYear();
        log.info("[ExpenseScheduler] 연별 통계 생성 시작: {}년", year);
        try {
            expenseService.generateYearlyStats(year);
            log.info("[ExpenseScheduler] 연별 통계 생성 완료");
        } catch (Exception e) {
            log.error("[ExpenseScheduler] 연별 통계 생성 실패: {}", e.getMessage());
        }
    }
}
