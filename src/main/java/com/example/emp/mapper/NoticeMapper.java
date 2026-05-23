package com.example.emp.mapper;

import com.example.emp.model.Notice;
import com.example.emp.model.NoticeRead;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoticeMapper {
    List<Notice> findAll();
    Notice findById(Long noticeId);
    int insert(Notice notice);
    int update(Notice notice);
    int delete(Long noticeId);

    // 관리자용: 공지 목록 + 읽은 사원 수
    List<Notice> findAllWithReadCount();

    // 사원용: 공지 목록 + 본인 읽음 여부
    List<Notice> findAllForUser(@Param("empno") Integer empno);

    // 읽음 처리 (중복 무시)
    void markRead(@Param("noticeId") Long noticeId, @Param("empno") Integer empno);

    // 공지별 읽은 사원 목록 (ename 포함)
    List<NoticeRead> findReaders(Long noticeId);

    // 공지별 안 읽은 사원 목록
    List<NoticeRead> findUnreaders(Long noticeId);

    // 사원의 미읽음 공지 수
    int countUnread(@Param("empno") Integer empno);
}
