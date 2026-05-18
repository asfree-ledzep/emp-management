package com.example.emp.controller;

import com.example.emp.model.SalaryStatDto;
import com.example.emp.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private StatsService statsService;

    // 부서별 급여 통계 조회 (평균, 최고, 최저, 사원 수)
    @GetMapping("/salary")
    public ResponseEntity<List<SalaryStatDto>> getSalaryStats() {
        return ResponseEntity.ok(statsService.getSalaryStats());
    }
}
