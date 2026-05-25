package com.example.emp.config;

import org.springframework.stereotype.Component;
import java.time.LocalTime;

/**
 * 당일 출근 마감 시간 설정 (관리자가 변경 가능, 서버 재시작 시 09:00으로 리셋)
 */
@Component
public class AttendanceConfigHolder {

    private volatile LocalTime checkInDeadline = LocalTime.of(9, 0); // 기본 09:00

    public LocalTime getCheckInDeadline() {
        return checkInDeadline;
    }

    public void setCheckInDeadline(LocalTime checkInDeadline) {
        this.checkInDeadline = checkInDeadline;
    }

    /** "HH:MM" 형식 문자열 반환 */
    public String getCheckInDeadlineStr() {
        return String.format("%02d:%02d", checkInDeadline.getHour(), checkInDeadline.getMinute());
    }
}
