package com.example.emp.mapper;

import com.example.emp.model.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMapper {

    /** 전체채팅 저장 (DEPTNO = NULL) */
    void insert(ChatMessage message);

    /** 부서채팅 저장 (DEPTNO 포함) */
    void insertDept(ChatMessage message);

    /** 전체채팅 최근 N건 조회 (오래된 순) */
    List<ChatMessage> findRecent(@Param("limit") int limit);

    /** 부서채팅 최근 N건 조회 (오래된 순) */
    List<ChatMessage> findRecentDept(@Param("deptno") int deptno, @Param("limit") int limit);
}
