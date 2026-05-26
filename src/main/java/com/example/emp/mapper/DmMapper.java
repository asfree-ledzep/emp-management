package com.example.emp.mapper;

import com.example.emp.model.DmMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DmMapper {
    void insert(DmMessage message);

    List<DmMessage> findHistory(
            @Param("empno1") Integer empno1,
            @Param("empno2") Integer empno2,
            @Param("limit")  int    limit
    );

    /** 나와 1회 이상 대화한 상대 사번 목록 (최근 대화 순) */
    List<Integer> findPartnerEmpnos(@Param("myEmpno") int myEmpno);
}
