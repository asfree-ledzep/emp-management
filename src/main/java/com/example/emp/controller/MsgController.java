package com.example.emp.controller;

import com.example.emp.mapper.EmpMapper;
import com.example.emp.model.Emp;
import com.example.emp.model.MsgMessage;
import com.example.emp.service.MsgService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 사내 쪽지함 API
 *
 * GET  /api/msg/inbox          — 받은 쪽지 목록
 * GET  /api/msg/sent           — 보낸 쪽지 목록
 * GET  /api/msg/{id}           — 쪽지 상세 (읽음 처리)
 * POST /api/msg/send           — 쪽지 발송 (multipart)
 * DELETE /api/msg/{id}         — 쪽지 삭제 (소프트)
 * GET  /api/msg/unread-count   — 미읽음 수
 * GET  /api/msg/emp-list       — 직원 목록 (받는 사람 선택용)
 */
@RestController
@RequestMapping("/api/msg")
public class MsgController {

    @Autowired private MsgService msgService;
    @Autowired private EmpMapper  empMapper;

    /* ── 현재 로그인 사용자 empno 추출 ────────── */
    private Integer getMyEmpno(Authentication auth) {
        try {
            return Integer.parseInt(auth.getName());
        } catch (NumberFormatException e) {
            return null; // ADMIN (사번 없음)
        }
    }

    /* ── 받은 쪽지 목록 ──────────────────────── */
    @GetMapping("/inbox")
    public ResponseEntity<?> inbox(Authentication auth) {
        Integer empno = getMyEmpno(auth);
        if (empno == null) return ResponseEntity.status(403).body(Map.of("error", "사번이 없는 관리자 계정"));
        return ResponseEntity.ok(msgService.getInbox(empno));
    }

    /* ── 보낸 쪽지 목록 ──────────────────────── */
    @GetMapping("/sent")
    public ResponseEntity<?> sent(Authentication auth) {
        Integer empno = getMyEmpno(auth);
        if (empno == null) return ResponseEntity.status(403).body(Map.of("error", "사번이 없는 관리자 계정"));
        return ResponseEntity.ok(msgService.getSent(empno));
    }

    /* ── 쪽지 상세 ────────────────────────────── */
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable long id, Authentication auth) {
        Integer empno = getMyEmpno(auth);
        if (empno == null) return ResponseEntity.status(403).body(Map.of("error", "사번 없음"));
        MsgMessage msg = msgService.getDetail(id, empno);
        if (msg == null) return ResponseEntity.notFound().build();
        // 권한 검사: 송신자 또는 수신자만 조회 가능
        if (!empno.equals(msg.getSenderEmpno()) && !empno.equals(msg.getReceiverEmpno())) {
            return ResponseEntity.status(403).body(Map.of("error", "권한 없음"));
        }
        return ResponseEntity.ok(msg);
    }

    /* ── 쪽지 발송 ────────────────────────────── */
    @PostMapping("/send")
    public ResponseEntity<?> send(
            @RequestParam("receiverEmpno") int receiverEmpno,
            @RequestParam("title")         String title,
            @RequestParam("content")       String content,
            @RequestParam(value = "parentMsgId", required = false) Long parentMsgId,
            @RequestParam(value = "files",       required = false) MultipartFile[] files,
            Authentication auth) {
        Integer senderEmpno = getMyEmpno(auth);
        if (senderEmpno == null) return ResponseEntity.status(403).body(Map.of("error", "사번 없음"));
        if (senderEmpno.equals(receiverEmpno)) {
            return ResponseEntity.badRequest().body(Map.of("error", "자신에게 쪽지를 보낼 수 없습니다."));
        }
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "제목을 입력하세요."));
        }
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "내용을 입력하세요."));
        }
        try {
            msgService.send(senderEmpno, title, content, receiverEmpno, parentMsgId, files);
            return ResponseEntity.ok(Map.of("message", "쪽지를 발송했습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /* ── 쪽지 삭제 ────────────────────────────── */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id, Authentication auth) {
        Integer empno = getMyEmpno(auth);
        if (empno == null) return ResponseEntity.status(403).body(Map.of("error", "사번 없음"));
        msgService.delete(id, empno);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    /* ── 미읽음 수 ────────────────────────────── */
    @GetMapping("/unread-count")
    public ResponseEntity<?> unreadCount(Authentication auth) {
        Integer empno = getMyEmpno(auth);
        if (empno == null) return ResponseEntity.ok(Map.of("count", 0));
        return ResponseEntity.ok(Map.of("count", msgService.countUnread(empno)));
    }

    /* ── 직원 목록 (받는 사람 선택용) ─────────── */
    @GetMapping("/emp-list")
    public ResponseEntity<?> empList(
            @RequestParam(defaultValue = "") String name,
            Authentication auth) {
        Integer myEmpno = getMyEmpno(auth);
        List<Map<String, Object>> list = empMapper.findAll().stream()
                .filter(e -> e.getEmpno() != null
                        && (myEmpno == null || !e.getEmpno().equals(myEmpno)))
                .filter(e -> name.isBlank() ||
                        (e.getEname() != null && e.getEname().contains(name)))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("empno",    e.getEmpno());
                    m.put("ename",    e.getEname()    != null ? e.getEname()    : "");
                    m.put("job",      e.getJob()      != null ? e.getJob()      : "");
                    m.put("deptno",   e.getDeptno()   != null ? e.getDeptno()   : 0);
                    m.put("photoUrl", e.getPhotoUrl() != null ? e.getPhotoUrl() : "");
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }
}
