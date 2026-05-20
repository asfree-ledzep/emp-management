package com.example.emp.mapper;

import com.example.emp.model.Dept;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DeptMapper {
    List<Dept> findAll();
    Dept findById(Integer deptno);
    int insert(Dept dept);
    int update(Dept dept);
    int delete(Integer deptno);
}
