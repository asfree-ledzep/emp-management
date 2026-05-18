package com.example.emp.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// 부서별 급여 통계 DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryStatDto {

    private Integer deptno;   // 부서번호
    private Double  avgSal;   // 평균 급여
    private Double  maxSal;   // 최고 급여
    private Double  minSal;   // 최저 급여
    private Integer empCount; // 사원 수
}
