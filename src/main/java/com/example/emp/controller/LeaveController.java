package com.example.emp.controller;

import com.example.emp.model.LeaveBalance;
import com.example.emp.model.LeaveRequest;
import com.example.emp.service.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // ── 사원: 잔여 연차 조회 ──
    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(Authentication auth) {
        try {
            Integer empno = Integer.parseInt(auth.getName());
            LeaveBalance bal = leaveService.getBalance(empno);
            return ResponseEntity.ok(bal);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── 사원: 본인 신청 내역 ──
    @GetMapping("/my")
    public ResponseEntity<?> getMyLeaves(Authentication auth) {
        try {
            Integer empno = Integer.parseInt(auth.getName());
            return ResponseEntity.ok(leaveService.getMyLeaves(empno));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── 사원: 연차 신청 ──
    @PostMapping
    public ResponseEntity<?> apply(@RequestBody LeaveRequest req, Authentication auth) {
        try {
            Integer empno = Integer.parseInt(auth.getName());
            leaveService.apply(empno, req);
            return ResponseEntity.ok(Map.of("message", "연차 신청이 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── 사원: 신청 취소 (PENDING만) ──
    @DeleteMapping("/{leaveId}")
    public ResponseEntity<?> cancel(@PathVariable Long leaveId, Authentication auth) {
        try {
            Integer empno = Integer.parseInt(auth.getName());
            leaveService.cancel(leaveId, empno);
            return ResponseEntity.ok(Map.of("message", "취소되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── MGR: 1차 승인 대기 목록 ──
    @GetMapping("/mgr/pending")
    public ResponseEntity<?> getPendingForMgr(Authentication auth) {
        try {
            Integer mgrEmpno = Integer.parseInt(auth.getName());
            return ResponseEntity.ok(leaveService.getPendingForMgr(mgrEmpno));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── MGR: 1차 승인 ──
    @PutMapping("/{leaveId}/mgr-approve")
    public ResponseEntity<?> mgrApprove(
            @PathVariable Long leaveId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        try {
            Integer mgrEmpno = Integer.parseInt(auth.getName());
            String comment = body != null ? body.getOrDefault("comment", "") : "";
            leaveService.mgrDecide(leaveId, mgrEmpno, true, comment);
            return ResponseEntity.ok(Map.of("message", "1차 승인 완료"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── MGR: 1차 반려 ──
    @PutMapping("/{leaveId}/mgr-reject")
    public ResponseEntity<?> mgrReject(
            @PathVariable Long leaveId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        try {
            Integer mgrEmpno = Integer.parseInt(auth.getName());
            String comment = body != null ? body.getOrDefault("comment", "") : "";
            leaveService.mgrDecide(leaveId, mgrEmpno, false, comment);
            return ResponseEntity.ok(Map.of("message", "1차 반려 완료"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── ADMIN: 전체 목록 ──
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAll(Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        try {
            return ResponseEntity.ok(leaveService.getAll());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── ADMIN: 특정 사원 잔여 연차 ──
    @GetMapping("/admin/balance/{empno}")
    public ResponseEntity<?> getBalanceAdmin(@PathVariable Integer empno, Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        try {
            return ResponseEntity.ok(leaveService.getBalanceForAdmin(empno));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── ADMIN: 최종 승인 ──
    @PutMapping("/{leaveId}/approve")
    public ResponseEntity<?> approve(
            @PathVariable Long leaveId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        try {
            String comment = body != null ? body.getOrDefault("comment", "") : "";
            leaveService.adminApprove(leaveId, comment);
            return ResponseEntity.ok(Map.of("message", "최종 승인 완료"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── ADMIN: 최종 반려 ──
    @PutMapping("/{leaveId}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Long leaveId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        try {
            String comment = body != null ? body.getOrDefault("comment", "") : "";
            leaveService.adminReject(leaveId, comment);
            return ResponseEntity.ok(Map.of("message", "최종 반려 완료"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
