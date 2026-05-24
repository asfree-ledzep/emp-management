package com.example.emp.mapper;

import com.example.emp.model.EmpKakao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface EmpKakaoMapper {
    void upsert(EmpKakao empKakao);
    List<EmpKakao> findAll();
    EmpKakao findByEmpno(Integer empno);
    void updateAccessToken(@Param("empno") Integer empno, @Param("accessToken") String accessToken);
    int countAll();

    // 카카오 연동 직원 목록 (ename 포함)
    List<Map<String, Object>> findConnectedWithName();

    // 카카오 미연동 직원 목록 (ename 포함)
    List<Map<String, Object>> findUnconnectedWithName();
}
