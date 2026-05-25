package com.example.emp.mapper;

import com.example.emp.model.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMapper {

    /** 메시지 저장 */
    void insert(ChatMessage message);

    /** 최근 N건 조회 (오래된 순) */
    List<ChatMessage> findRecent(@Param("limit") int limit);
}
