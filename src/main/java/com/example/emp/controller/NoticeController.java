package com.example.emp.controller;

import com.example.emp.model.Notice;
import com.example.emp.model.NoticeReadSummaryDto;
import com.example.emp.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    // 전체 조회 (role에 따라 readCount / isRead 포함)
    @GetMapping
    public ResponseEntity<List<Notice>> getAll(Authentication auth) {
        if (isAdmin(auth)) {
            return ResponseEntity.ok(noticeService.getAllWithReadCount());
        }
        Integer empno = Integer.parseInt(auth.getName());
        return ResponseEntity.ok(noticeService.getAllForUser(empno));
    }

    // 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Notice notice = noticeService.getById(id);
        if (notice == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(notice);
    }

    // 사원 미읽음 공지 수 (path literal → /{id} 보다 먼저 매핑됨)
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(Authentication auth) {
        if (isAdmin(auth)) return ResponseEntity.ok(Map.of("count", 0));
        Integer empno = Integer.parseInt(auth.getName());
        return ResponseEntity.ok(Map.of("count", noticeService.countUnread(empno)));
    }

    // 읽음 처리 (사원 호출)
    @PostMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id, Authentication auth) {
        if (!isAdmin(auth)) {
            Integer empno = Integer.parseInt(auth.getName());
            noticeService.markRead(id, empno);
        }
        return ResponseEntity.ok().build();
    }

    // 읽음 현황 (관리자 전용)
    @GetMapping("/{id}/reads")
    public ResponseEntity<?> getReadSummary(@PathVariable Long id, Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        NoticeReadSummaryDto summary = noticeService.getReadSummary(id);
        return ResponseEntity.ok(summary);
    }

    // 등록 (관리자만)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Notice notice, Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(Map.of("error", "관리자만 공지 작성 가능합니다."));
        notice.setCreatedBy(auth.getName());
        noticeService.create(notice);
        return ResponseEntity.ok(Map.of("message", "공지사항 등록 완료"));
    }

    // 수정 (관리자만)
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Notice notice, Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(Map.of("error", "관리자만 수정 가능합니다."));
        noticeService.update(id, notice);
        return ResponseEntity.ok(Map.of("message", "공지사항 수정 완료"));
    }

    // 삭제 (관리자만)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(Map.of("error", "관리자만 삭제 가능합니다."));
        noticeService.delete(id);
        return ResponseEntity.ok(Map.of("message", "공지사항 삭제 완료"));
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
