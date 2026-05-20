package com.example.emp.mapper;

import com.example.emp.model.Emp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmpMapper {

    List<Emp> findAll();

    Emp findById(Integer empno);

    int insert(Emp emp);

    int update(Emp emp);

    int delete(Integer empno);

    List<Emp> findByDeptno(Integer deptno);

    int updatePhotoUrl(@Param("empno") Integer empno, @Param("photoUrl") String photoUrl);
}
