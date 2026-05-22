package com.example.emp.service;

import com.example.emp.mapper.NoticeMapper;
import com.example.emp.model.Notice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoticeService {

    @Autowired private NoticeMapper noticeMapper;
    @Autowired private KakaoService kakaoService;

    public List<Notice> getAll() {
        return noticeMapper.findAll();
    }

    public Notice getById(Long id) {
        return noticeMapper.findById(id);
    }

    public void create(Notice notice) {
        noticeMapper.insert(notice);
        // 카카오 연동된 사원에게 메시지 발송
        kakaoService.sendMessageToAll(
            "📢 [공지사항] " + notice.getTitle(),
            notice.getContent(),
            "https://emp-management-react.vercel.app/?page=notice"
        );
    }

    public void delete(Long id) {
        noticeMapper.delete(id);
    }
}
