package com.example.emp.service;

import com.example.emp.mapper.NoticeMapper;
import com.example.emp.model.Notice;
import com.example.emp.model.NoticeRead;
import com.example.emp.model.NoticeReadSummaryDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeService {

    @Autowired private NoticeMapper noticeMapper;
    @Autowired private KakaoService kakaoService;

    // 관리자용: 읽은 수 포함
    public List<Notice> getAllWithReadCount() {
        return noticeMapper.findAllWithReadCount();
    }

    // 사원용: 본인 읽음 여부 포함
    public List<Notice> getAllForUser(Integer empno) {
        return noticeMapper.findAllForUser(empno);
    }

    // 하위 호환 (기존 호출 유지)
    public List<Notice> getAll() {
        return noticeMapper.findAll();
    }

    public Notice getById(Long id) {
        return noticeMapper.findById(id);
    }

    public void create(Notice notice) {
        noticeMapper.insert(notice);
        kakaoService.sendMessageToAll(
            "📢 [공지사항] " + notice.getTitle(),
            notice.getContent(),
            "https://emp-management-react.vercel.app/?page=notice"
        );
    }

    public void update(Long id, Notice notice) {
        notice.setNoticeId(id);
        noticeMapper.update(notice);
    }

    public void delete(Long id) {
        noticeMapper.delete(id);
    }

    // 읽음 처리 (중복 무시)
    public void markRead(Long noticeId, Integer empno) {
        noticeMapper.markRead(noticeId, empno);
    }

    // 공지별 읽음 현황 요약 (관리자용)
    public NoticeReadSummaryDto getReadSummary(Long noticeId) {
        List<NoticeRead> readers   = noticeMapper.findReaders(noticeId);
        List<NoticeRead> unreaders = noticeMapper.findUnreaders(noticeId);
        NoticeReadSummaryDto dto = new NoticeReadSummaryDto();
        dto.setReadCount(readers.size());
        dto.setTotalEmp(readers.size() + unreaders.size());
        dto.setReaders(readers);
        dto.setUnreaders(unreaders);
        return dto;
    }

    // 사원의 미읽음 공지 수
    public int countUnread(Integer empno) {
        return noticeMapper.countUnread(empno);
    }
}
