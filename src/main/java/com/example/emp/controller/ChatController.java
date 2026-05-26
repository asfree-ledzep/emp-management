package com.example.emp.controller;

import com.example.emp.mapper.ChatMapper;
import com.example.emp.model.ChatMessage;
import com.example.emp.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
    public ResponseEntity<List<ChatMessage>> history(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(chatMapper.findRecent(limit));
    }

    /* ── 부서채팅 이력 조회 ── */
    @GetMapping("/api/chat/dept/history")
    public ResponseEntity<List<ChatMessage>> deptHistory(
            @RequestParam int deptno,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(chatMapper.findRecentDept(deptno, limit));
    }

    /**
     * 채팅 파일 다운로드 프록시
     * S3 버킷 정책이 chat/ 경로를 막는 경우에도 백엔드를 통해 스트리밍
     * GET /api/chat/download?url=https://...
     */
    @GetMapping("/api/chat/download")
    public ResponseEntity<byte[]> downloadChatFile(@RequestParam String url) {
        try {
            URI uri = new URI(url);
            // chat/ 경로만 허용 (보안: 임의 URL 프록시 방지)
            if (!uri.getPath().contains("/chat/")) {
                return ResponseEntity.badRequest().build();
            }
            try (InputStream in = uri.toURL().openStream()) {
                byte[] data = in.readAllBytes();
                // 파일명 추출 (URL 마지막 세그먼트에서 타임스탬프_ 제거)
                String path     = uri.getPath();
                String rawName  = path.substring(path.lastIndexOf('/') + 1);
                // 타임스탬프_파일명 형식에서 파일명만 추출
                String fileName = rawName.contains("_") ? rawName.substring(rawName.indexOf('_') + 1) : rawName;
                String encoded  = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encoded)
                        .header(HttpHeaders.CACHE_CONTROL, "no-store")
                        .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                        .body(data);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
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
