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
}
