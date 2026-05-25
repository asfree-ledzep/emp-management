package com.example.emp.controller;

import com.example.emp.config.AttendanceConfigHolder;
import com.example.emp.model.Attendance;
import com.example.emp.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceConfigHolder attendanceConfig;

    public AttendanceController(AttendanceService attendanceService,
                                AttendanceConfigHolder attendanceConfig) {
        this.attendanceService = attendanceService;
        this.attendanceConfig  = attendanceConfig;
    }

    /**
     * 직원 자신의 월별 출근 기록
     * GET /api/attendance/my?empno=7900&month=2025-05
     */
    @GetMapping("/my")
    public ResponseEntity<List<Attendance>> my(
            @RequestParam Integer empno,
            @RequestParam(required = false) String month) {
        if (month == null || month.isBlank()) {
            month = java.time.YearMonth.now(ZoneId.of("Asia/Seoul")).toString();
        }
        return ResponseEntity.ok(attendanceService.getMyRecords(empno, month));
    }

    /**
     * 관리자: 전체 출근 기록 조회
     * GET /api/attendance/admin?date=2025-05-01&deptno=10
     */
    @GetMapping("/admin")
    public ResponseEntity<List<Attendance>> admin(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer deptno) {
        return ResponseEntity.ok(attendanceService.getAll(date, deptno));
    }

    /**
     * 오늘 전체 출근 현황 (QR 화면 옆 목록)
     * GET /api/attendance/today
     */
    @GetMapping("/today")
    public ResponseEntity<List<Attendance>> today() {
        return ResponseEntity.ok(attendanceService.getTodayAll());
    }

    /**
     * 오늘 미출근 사원 목록
     * GET /api/attendance/absent-today
     */
    @GetMapping("/absent-today")
    public ResponseEntity<List<Attendance>> absentToday() {
        return ResponseEntity.ok(attendanceService.getAbsentToday());
    }

    /**
     * 월별 직원별 근태 누계 (근퇴불량자 분석)
     * GET /api/attendance/monthly-stats?month=2025-05
     */
    @GetMapping("/monthly-stats")
    public ResponseEntity<List<Map<String, Object>>> monthlyStats(
            @RequestParam(required = false) String month) {
        return ResponseEntity.ok(attendanceService.getMonthlyStats(month));
    }

    /**
     * 출근 마감 시간 조회
     * GET /api/attendance/config/checkin-time
     */
    @GetMapping("/config/checkin-time")
    public ResponseEntity<Map<String, String>> getCheckInTime() {
        return ResponseEntity.ok(Map.of("checkInDeadline", attendanceConfig.getCheckInDeadlineStr()));
    }

    /**
     * 출근 마감 시간 변경 (관리자)
     * POST /api/attendance/config/checkin-time?time=10:30
     */
    @PostMapping("/config/checkin-time")
    public ResponseEntity<Map<String, String>> setCheckInTime(@RequestParam String time) {
        try {
            String[] parts = time.split(":");
            LocalTime deadline = LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            attendanceConfig.setCheckInDeadline(deadline);
            return ResponseEntity.ok(Map.of(
                "checkInDeadline", attendanceConfig.getCheckInDeadlineStr(),
                "message", "출근 마감 시간이 " + attendanceConfig.getCheckInDeadlineStr() + "(으)로 변경되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "시간 형식이 잘못되었습니다. HH:MM 형식으로 입력하세요."));
        }
    }
}
