package com.example.emp.controller;

import com.example.emp.service.KakaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;



@RestController
@RequestMapping("/api/kakao")
public class KakaoController {

    @Autowired
    private KakaoService kakaoService;

    // 카카오 로그인 URL 반환
    @GetMapping("/auth-url")
    public ResponseEntity<?> getAuthUrl() {
        return ResponseEntity.ok(Map.of("url", kakaoService.getAuthUrl()));
    }

    // 카카오 연동 사원 수 조회
    @GetMapping("/connected-count")
    public ResponseEntity<?> getConnectedCount() {
        return ResponseEntity.ok(Map.of("count", kakaoService.countConnected()));
    }

    // 연동/미연동 직원 현황 조회 (관리자 전용)
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(kakaoService.getStatus());
    }

    // 미연동 직원 독려 메시지 발송 (관리자 전용)
    @PostMapping("/nudge")
    public ResponseEntity<?> sendNudge(Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        int sent = kakaoService.sendNudge();
        return ResponseEntity.ok(Map.of(
            "message", "독려 메시지 발송 완료",
            "sentCount", sent
        ));
    }

    // 콜백: 코드로 토큰 교환 + DB 저장
    @PostMapping("/callback")
    public ResponseEntity<?> callback(@RequestBody Map<String, String> body, Authentication auth) {
        // 관리자는 카카오 연동 불필요
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return ResponseEntity.badRequest().body(Map.of("error", "관리자는 카카오 연동이 필요없습니다."));
        }

        String code = body.get("code");
        Integer empno = Integer.parseInt(auth.getName());

        try {
            kakaoService.connectKakao(code, empno);
            return ResponseEntity.ok(Map.of("message", "카카오 연동 완료"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
