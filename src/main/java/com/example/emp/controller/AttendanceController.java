package com.example.emp.controller;

import com.example.emp.model.Attendance;
import com.example.emp.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
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
            java.time.YearMonth ym = java.time.YearMonth.now();
            month = ym.toString(); // "YYYY-MM"
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
}
