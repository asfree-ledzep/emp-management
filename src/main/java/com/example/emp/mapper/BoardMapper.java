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
    List<BoardPost> findAll();                                                          // 관리자 전체 조회
    BoardPost findById(@Param("postId") long postId);
    int updateByIdAndEmpno(BoardPost post);                                             // 본인 글 수정
    int deleteByIdAndEmpno(@Param("postId") long postId, @Param("empno") int empno);
    int deleteById(@Param("postId") long postId);                                       // 관리자 삭제
}
