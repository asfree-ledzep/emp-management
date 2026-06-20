package com.example.emp.mapper;

import com.example.emp.model.Todo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TodoMapper {
    void insert(Todo todo);
    void update(Todo todo);
    void toggle(@Param("todoId") Long todoId, @Param("empno") Integer empno);
    void delete(@Param("todoId") Long todoId, @Param("empno") Integer empno);
    void updateOrder(@Param("todoId") Long todoId, @Param("sortOrder") int sortOrder);
    List<Todo> findByEmpno(Integer empno);
    Todo findById(@Param("todoId") Long todoId, @Param("empno") Integer empno);
    long nextVal();
}
