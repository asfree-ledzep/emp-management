package com.example.emp.controller;

import com.example.emp.mapper.ChatMapper;
import com.example.emp.model.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * WebSocket 채팅 컨트롤러
 *
 * 클라이언트 → /app/chat.send → 서버 저장 → /topic/chat 브로드캐스트
 * REST       → GET /api/chat/history  (최근 메시지 이력)
 */
@Controller
public class ChatController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter HHmm = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired private ChatMapper           chatMapper;
    @Autowired private SimpMessagingTemplate messaging;

    /**
     * 메시지 수신 → DB 저장 → 전체 브로드캐스트
     * STOMP destination: /app/chat.send
     */
    @MessageMapping("/chat.send")
    public void handleMessage(ChatMessage message) {
        // 서버 시간 (KST) 설정
        message.setSentAt(LocalTime.now(KST).format(HHmm));

        // DB 저장
        chatMapper.insert(message);

        // 모든 구독자에게 브로드캐스트
        messaging.convertAndSend("/topic/chat", message);
    }

    /**
     * 최근 채팅 이력 조회 (REST)
     * GET /api/chat/history?limit=50
     */
    @GetMapping("/api/chat/history")
    @ResponseBody
    public List<ChatMessage> history(
            @RequestParam(defaultValue = "50") int limit) {
        return chatMapper.findRecent(limit);
    }
}
