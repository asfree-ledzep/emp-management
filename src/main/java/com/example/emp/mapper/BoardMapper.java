package com.example.emp.mapper;

import com.example.emp.model.BoardPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BoardMapper {
    void insertGlobal(BoardPost post);
    void insertDept(BoardPost post);
    List<BoardPost> findGlobal();
    List<BoardPost> findByDept(@Param("deptno") int deptno);
    BoardPost findById(@Param("postId") long postId);
    int deleteByIdAndEmpno(@Param("postId") long postId, @Param("empno") int empno);
}
