package com.example.emp.controller;

import com.example.emp.mapper.ChatMapper;
import com.example.emp.model.ChatMessage;
import com.example.emp.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 채팅 컨트롤러
 *
 * [전체채팅]  /app/chat.send  → /topic/chat         (DEPTNO NULL)
 * [부서채팅]  /app/dept.send  → /topic/dept/{deptno} (DEPTNO 있음)
 * REST: GET /api/chat/history            — 전체채팅 이력
 *       GET /api/chat/dept/history       — 부서채팅 이력
 *       POST /api/chat/upload            — 파일 S3 업로드 → URL 반환
 */
@Controller
public class ChatController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter HHmm = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired private ChatMapper           chatMapper;
    @Autowired private SimpMessagingTemplate messaging;
    @Autowired private S3Service            s3Service;

    /* ── 전체채팅: /app/chat.send → DB 저장 → /topic/chat 브로드캐스트 ── */
    @MessageMapping("/chat.send")
    public void handleMessage(ChatMessage message) {
        message.setSentAt(LocalTime.now(KST).format(HHmm));
        message.setDeptno(null);   // 전체채팅 = DEPTNO NULL
        chatMapper.insert(message);
        messaging.convertAndSend("/topic/chat", message);
    }

    /* ── 부서채팅: /app/dept.send → DB 저장 → /topic/dept/{deptno} 브로드캐스트 ── */
    @MessageMapping("/dept.send")
    public void handleDeptMessage(ChatMessage message) {
        if (message.getDeptno() == null) return;
        message.setSentAt(LocalTime.now(KST).format(HHmm));
        chatMapper.insertDept(message);
        messaging.convertAndSend("/topic/dept/" + message.getDeptno(), message);
    }

    /* ── 전체채팅 이력 조회 ── */
    @GetMapping("/api/chat/history")
    @ResponseBody
    public List<ChatMessage> history(
            @RequestParam(defaultValue = "50") int limit) {
        return chatMapper.findRecent(limit);
    }

    /* ── 부서채팅 이력 조회 ── */
    @GetMapping("/api/chat/dept/history")
    @ResponseBody
    public List<ChatMessage> deptHistory(
            @RequestParam int deptno,
            @RequestParam(defaultValue = "50") int limit) {
        return chatMapper.findRecentDept(deptno, limit);
    }

    /* ── 채팅 파일 업로드 → S3 저장 → { url, fileName } 반환 ── */
    @PostMapping("/api/chat/upload")
    @ResponseBody
    public ResponseEntity<?> uploadChatFile(
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        try {
            Integer empno = 0;
            try { empno = Integer.parseInt(auth.getName()); } catch (NumberFormatException ignored) {}
            String url = s3Service.uploadChatFile(empno, file);
            return ResponseEntity.ok(Map.of(
                    "url",      url,
                    "fileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "파일 업로드 실패: " + e.getMessage()));
        }
    }
}
