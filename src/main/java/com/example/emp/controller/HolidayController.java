package com.example.emp.controller;

import com.example.emp.mapper.HolidayMapper;
import com.example.emp.model.Holiday;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    @Autowired
    private HolidayMapper holidayMapper;

    // 연도별 전체 조회 (관리자 전용)
    @GetMapping
    public ResponseEntity<List<Holiday>> getByYear(
            @RequestParam(defaultValue = "0") int year,
            Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        int y = year > 0 ? year : java.time.LocalDate.now().getYear();
        return ResponseEntity.ok(holidayMapper.findByYear(y));
    }

    // 연월별 조회 (모든 사용자 — 챗봇에서 직접 호출 가능)
    @GetMapping("/month")
    public ResponseEntity<List<Holiday>> getByMonth(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(holidayMapper.findByYearMonth(year, month));
    }

    // 등록 (관리자만)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Holiday holiday, Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(Map.of("error", "관리자만 등록 가능합니다."));
        holiday.setCreatedBy(auth.getName());
        holidayMapper.insert(holiday);
        return ResponseEntity.ok(Map.of("message", "공휴일 등록 완료"));
    }

    // 삭제 (관리자만)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(Map.of("error", "관리자만 삭제 가능합니다."));
        holidayMapper.delete(id);
        return ResponseEntity.ok(Map.of("message", "공휴일 삭제 완료"));
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
