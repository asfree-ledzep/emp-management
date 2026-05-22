package com.example.emp.mapper;

import com.example.emp.model.Notice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoticeMapper {
    List<Notice> findAll();
    Notice findById(Long noticeId);
    int insert(Notice notice);
    int delete(Long noticeId);
}
