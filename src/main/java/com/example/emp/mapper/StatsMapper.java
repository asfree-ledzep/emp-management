package com.example.emp.mapper;

import com.example.emp.model.SalaryStatDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StatsMapper {

    // 부서별 급여 통계 (평균, 최고, 최저, 사원 수)
    List<SalaryStatDto> findSalaryStats();
}
