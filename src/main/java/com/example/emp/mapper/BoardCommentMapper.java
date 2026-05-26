package com.example.emp.mapper;

import com.example.emp.model.BoardComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BoardCommentMapper {
    void insert(BoardComment comment);
    List<BoardComment> findByPostId(@Param("postId") long postId);
    int deleteByIdAndEmpno(@Param("commentId") long commentId, @Param("empno") int empno);
}
