package com.example.emp.controller;

import com.example.emp.model.Notice;
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

    // 전체 조회 (모든 사용자)
    @GetMapping
    public ResponseEntity<List<Notice>> getAll() {
        return ResponseEntity.ok(noticeService.getAll());
    }

    // 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Notice notice = noticeService.getById(id);
        if (notice == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(notice);
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
