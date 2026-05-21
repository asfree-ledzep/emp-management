package com.example.emp.controller;

import com.example.emp.model.PushSubscription;
import com.example.emp.service.PushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
public class PushController {

    @Autowired
    private PushService pushService;

    // 구독 등록
    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody PushSubscription sub) {
        pushService.subscribe(sub);
        return ResponseEntity.ok(Map.of("message", "구독 완료"));
    }

    // 구독 해제
    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe(@RequestBody Map<String, String> body) {
        pushService.unsubscribe(body.get("endpoint"));
        return ResponseEntity.ok(Map.of("message", "구독 해제 완료"));
    }

    // 테스트용: 즉시 전체 발송 (관리자만)
    @PostMapping("/test")
    public ResponseEntity<?> testPush(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "테스트 알림");
        String msg   = body.getOrDefault("body",  "푸시 알림 테스트입니다.");
        pushService.sendToAll(title, msg);
        return ResponseEntity.ok(Map.of("message", "푸시 발송 완료"));
    }
}
