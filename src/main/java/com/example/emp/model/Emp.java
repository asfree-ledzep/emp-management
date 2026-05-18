package com.example.emp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Emp {

    private Integer empno;    // 사번
    private String  ename;    // 이름
    private String  job;      // 직책
    private Integer mgr;      // 상사 사번
    private LocalDate hiredate; // 입사일
    private Double  sal;      // 급여
    private Double  comm;     // 커미션
    private Integer deptno;   // 부서번호
}
