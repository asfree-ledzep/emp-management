package com.example.emp.service;

import com.example.emp.mapper.StatsMapper;
import com.example.emp.model.SalaryStatDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StatsService {

    @Autowired
    private StatsMapper statsMapper;

    // 부서별 급여 통계 조회
    public List<SalaryStatDto> getSalaryStats() {
        return statsMapper.findSalaryStats();
    }
}
