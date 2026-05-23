package com.example.emp.controller;

import com.example.emp.model.Holiday;
import com.example.emp.service.PublicHolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    @Autowired
    private PublicHolidayService publicHolidayService;

    // 연월별 공휴일 조회 — 공공데이터포털 한국천문연구원 특일 정보 API 실시간 호출
    @GetMapping("/month")
    public ResponseEntity<List<Holiday>> getByMonth(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(publicHolidayService.getHolidays(year, month));
    }
}
