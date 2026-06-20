package com.example.emp.controller;

import com.example.emp.model.WorkLog;
import com.example.emp.service.WorkLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/worklog")
public class WorkLogController {

    @Autowired private WorkLogService workLogService;

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // ─── 사원: 임시저장 ─────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> save(@RequestBody WorkLog req, Authentication auth) {
        try {
            Integer empno = Integer.parseInt(auth.getName());
            return ResponseEntity.ok(workLogService.save(empno, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── 사원: 수정 ─────────────────────────────────────────
    @PutMapping("/{logId}")
    public ResponseEntity<?> edit(@PathVariable Long logId,
                                  @RequestBody WorkLog req, Authentication auth) {
        try {
            Integer empno = Integer.parseInt(auth.getName());
            return ResponseEntity.ok(workLogService.edit(empno, logId, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── 사원: 제출 ─────────────────────────────────────────
    @PostMapping("/{logId}/submit")
    public ResponseEntity<?> submit(@PathVariable Long logId, Authentication auth) {
        try {
            Integer empno = Integer.parseInt(auth.getName());
            workLogService.submit(empno, logId);
            return ResponseEntity.ok(Map.of("message", "결재 제출이 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── 사원: 삭제 ─────────────────────────────────────────
    @DeleteMapping("/{logId}")
    public ResponseEntity<?> delete(@PathVariable Long logId, Authentication auth) {
        try {
            Integer empno = Integer.parseInt(auth.getName());
            workLogService.delete(empno, logId);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── 사원: 본인 목록 ────────────────────────────────────
    @GetMapping("/my")
    public ResponseEntity<?> getMyLogs(Authentication auth) {
        try {
            Integer empno = Integer.parseInt(auth.getName());
            return ResponseEntity.ok(workLogService.getMyLogs(empno));
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(java.util.List.of()); // 관리자: 빈 목록
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── 단건 조회 ──────────────────────────────────────────
    @GetMapping("/{logId}")
    public ResponseEntity<?> getById(@PathVariable Long logId) {
        try {
            WorkLog log = workLogService.getById(logId);
            if (log == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(log);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── 팀장: 결재 대기 목록 ───────────────────────────────
    @GetMapping("/mgr/pending")
    public ResponseEntity<?> mgrPending(Authentication auth) {
        try {
            Integer mgrEmpno = Integer.parseInt(auth.getName());
            return ResponseEntity.ok(workLogService.getPendingForMgr(mgrEmpno));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── 팀장: 팀원 전체 목록 ───────────────────────────────
    @GetMapping("/mgr/all")
    public ResponseEntity<?> mgrAll(Authentication auth) {
        try {
            Integer mgrEmpno = Integer.parseInt(auth.getName());
            return ResponseEntity.ok(workLogService.getAllForMgr(mgrEmpno));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── 팀장: 승인 ─────────────────────────────────────────
    @PostMapping("/{logId}/mgr-approve")
    public ResponseEntity<?> mgrApprove(@PathVariable Long logId,
                                        @RequestBody(required = false) Map<String,String> body,
                                        Authentication auth) {
        try {
            Integer mgrEmpno = Integer.parseInt(auth.getName());
            String comment = body != null ? body.get("comment") : null;
            workLogService.mgrApprove(mgrEmpno, logId, comment);
            return ResponseEntity.ok(Map.of("message", "승인되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── 팀장: 반려 ─────────────────────────────────────────
    @PostMapping("/{logId}/mgr-reject")
    public ResponseEntity<?> mgrReject(@PathVariable Long logId,
                                       @RequestBody Map<String,String> body,
                                       Authentication auth) {
        try {
            Integer mgrEmpno = Integer.parseInt(auth.getName());
            String reason  = body.getOrDefault("reason", "");
            String comment = body.get("comment");
            if (reason.isBlank()) return ResponseEntity.badRequest()
                    .body(Map.of("error", "반려 사유를 입력해주세요."));
            workLogService.mgrReject(mgrEmpno, logId, reason, comment);
            return ResponseEntity.ok(Map.of("message", "반려 처리되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── 관리자: 확인 대기 목록 ─────────────────────────────
    @GetMapping("/admin/pending")
    public ResponseEntity<?> adminPending(Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(Map.of("error","권한 없음"));
        try {
            return ResponseEntity.ok(workLogService.getPendingForAdmin());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── 관리자: 전체 목록 ──────────────────────────────────
    @GetMapping("/admin/all")
    public ResponseEntity<?> adminAll(Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(Map.of("error","권한 없음"));
        try {
            return ResponseEntity.ok(workLogService.getAllLogs());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── 관리자: 최종 확인 ──────────────────────────────────
    @PostMapping("/{logId}/admin-approve")
    public ResponseEntity<?> adminApprove(@PathVariable Long logId,
                                          @RequestBody(required = false) Map<String,String> body,
                                          Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(Map.of("error","권한 없음"));
        try {
            String comment = body != null ? body.get("comment") : null;
            workLogService.adminApprove(logId, comment);
            return ResponseEntity.ok(Map.of("message", "확인 처리되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── 관리자: 반려 ───────────────────────────────────────
    @PostMapping("/{logId}/admin-reject")
    public ResponseEntity<?> adminReject(@PathVariable Long logId,
                                         @RequestBody Map<String,String> body,
                                         Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(Map.of("error","권한 없음"));
        try {
            String reason  = body.getOrDefault("reason", "");
            String comment = body.get("comment");
            if (reason.isBlank()) return ResponseEntity.badRequest()
                    .body(Map.of("error", "반려 사유를 입력해주세요."));
            workLogService.adminReject(logId, reason, comment);
            return ResponseEntity.ok(Map.of("message", "반려 처리되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
