package com.example.emp.service;

import com.example.emp.config.AttendanceConfigHolder;
import com.example.emp.mapper.AttendanceMapper;
import com.example.emp.model.Attendance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceService {

    private final AttendanceMapper attendanceMapper;
    private final AttendanceConfigHolder attendanceConfig;

    public AttendanceService(AttendanceMapper attendanceMapper,
                             AttendanceConfigHolder attendanceConfig) {
        this.attendanceMapper = attendanceMapper;
        this.attendanceConfig = attendanceConfig;
    }

    /**
     * QR 스캔 처리.
     * 오전(13시 미만) → 출근, 오후(13시 이상) → 퇴근
     */
    @Transactional
    public Map<String, Object> scan(Integer empno) {
        LocalTime now = LocalTime.now();
        Attendance today = attendanceMapper.findTodayByEmpno(empno);

        if (now.getHour() < 13) {
            // 출근 처리
            if (today != null) {
                return Map.of("result", "ALREADY_CHECKED_IN",
                              "message", "이미 오늘 출근 처리가 되어 있습니다.",
                              "checkIn", today.getCheckIn());
            }
            // 관리자 설정 출근 마감 시간 기준으로 지각 여부 판단
            LocalTime deadline = attendanceConfig.getCheckInDeadline();
            String status = now.isAfter(deadline) ? "LATE" : "NORMAL";
            Attendance attendance = new Attendance();
            attendance.setEmpno(empno);
            attendance.setStatus(status);
            attendanceMapper.insertCheckIn(attendance);
            today = attendanceMapper.findTodayByEmpno(empno);
            return Map.of("result", "CHECK_IN",
                          "message", "출근이 기록되었습니다.",
                          "checkIn", today.getCheckIn(),
                          "status", today.getStatus());
        } else {
            // 퇴근 처리
            if (today == null) {
                return Map.of("result", "NO_CHECK_IN",
                              "message", "오늘 출근 기록이 없습니다. 관리자에게 문의하세요.");
            }
            if (today.getCheckOut() != null && !today.getCheckOut().isEmpty()) {
                return Map.of("result", "ALREADY_CHECKED_OUT",
                              "message", "이미 퇴근 처리가 되어 있습니다.",
                              "checkOut", today.getCheckOut());
            }
            attendanceMapper.updateCheckOut(empno);
            today = attendanceMapper.findTodayByEmpno(empno);
            return Map.of("result", "CHECK_OUT",
                          "message", "퇴근이 기록되었습니다.",
                          "checkOut", today.getCheckOut(),
                          "workMinutes", today.getWorkMinutes() != null ? today.getWorkMinutes() : 0,
                          "status", today.getStatus());
        }
    }

    /** 직원 월별 기록 */
    public List<Attendance> getMyRecords(Integer empno, String month) {
        return attendanceMapper.findByEmpnoAndMonth(empno, month);
    }

    /** 관리자 전체 조회 */
    public List<Attendance> getAll(String date, Integer deptno) {
        return attendanceMapper.findAll(date, deptno);
    }

    /** 오늘 전체 현황 */
    public List<Attendance> getTodayAll() {
        return attendanceMapper.findTodayAll();
    }

    /** 오늘 미출근 사원 */
    public List<Attendance> getAbsentToday() {
        return attendanceMapper.findAbsentToday();
    }

    /** 월별 직원별 근태 누계 (근태불량자 분석) */
    public List<Map<String, Object>> getMonthlyStats(String month) {
        if (month == null || month.isBlank()) {
            month = java.time.YearMonth.now().toString();
        }
        return attendanceMapper.findMonthlyStats(month);
    }
}
