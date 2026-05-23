package com.example.emp.mapper;

import com.example.emp.model.EmpAddr;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmpAddrMapper {
    EmpAddr findByEmpno(Integer empno);
    int upsert(EmpAddr addr);
    int deleteByEmpno(Integer empno);
}
