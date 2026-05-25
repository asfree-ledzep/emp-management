package com.example.emp.controller;

import com.example.emp.model.QrToken;
import com.example.emp.service.AttendanceService;
import com.example.emp.service.QrService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/qr")
public class QrController {

    private final QrService qrService;
    private final AttendanceService attendanceService;

    public QrController(QrService qrService, AttendanceService attendanceService) {
        this.qrService = qrService;
        this.attendanceService = attendanceService;
    }

    /**
     * 관리자 화면: 현재 유효한 QR 토큰 반환 (없으면 새로 생성)
     * GET /api/qr/current
     */
    @GetMapping("/current")
    public ResponseEntity<QrToken> getCurrent() {
        QrToken token = qrService.getCurrentToken();
        return ResponseEntity.ok(token);
    }

    /**
     * 직원 QR 스캔: 토큰 검증 후 출퇴근 기록
     * POST /api/qr/scan?token=<UUID>&empno=<empno>
     */
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scan(
            @RequestParam String token,
            @RequestParam Integer empno) {

        QrToken valid = qrService.validateToken(token);
        if (valid == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", "INVALID_TOKEN",
                                 "message", "QR 코드가 만료되었거나 유효하지 않습니다."));
        }

        Map<String, Object> result = attendanceService.scan(empno);
        return ResponseEntity.ok(result);
    }
}
