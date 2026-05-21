package com.example.emp.mapper;

import com.example.emp.model.EmpAuth;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmpAuthMapper {

    // 사번으로 인증 정보 조회
    EmpAuth findByEmpno(Integer empno);

    // 비밀번호 등록 또는 갱신 (MERGE INTO)
    void upsert(EmpAuth empAuth);
}
