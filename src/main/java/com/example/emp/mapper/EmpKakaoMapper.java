package com.example.emp.mapper;

import com.example.emp.model.EmpKakao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmpKakaoMapper {
    void upsert(EmpKakao empKakao);
    List<EmpKakao> findAll();
    EmpKakao findByEmpno(Integer empno);
    void updateAccessToken(@Param("empno") Integer empno, @Param("accessToken") String accessToken);
    int countAll();
}
