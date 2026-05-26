package com.example.emp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class AdminProfile {
    private String  displayName;   // 표시될 이름
    private String  empnoDisplay;  // 사번 (표시용)
    private Integer deptno;        // 부서
    private String  pwHash;        // BCrypt 해시 (조회 시 null 반환)
}
