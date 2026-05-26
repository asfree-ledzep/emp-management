package com.example.emp.controller;

import com.example.emp.mapper.AdminMapper;
import com.example.emp.mapper.EmpMapper;
import com.example.emp.model.AdminProfile;
import com.example.emp.model.Emp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관리자 프로필 · 대리관리자 관리
 * GET  /api/admin/profile          — 관리자 프로필 조회
 * PUT  /api/admin/profile          — 관리자 프로필 수정 (이름/사번/부서/비번)
 * GET  /api/admin/proxy            — 대리 관리자 목록
 * POST /api/admin/proxy/{empno}    — 대리 관리자 추가
 * DELETE /api/admin/proxy/{empno}  — 대리 관리자 제거
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private AdminMapper    adminMapper;
    @Autowired private EmpMapper      empMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /* ── 프로필 조회 ── */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        AdminProfile p = adminMapper.findProfile();
        if (p == null) p = new AdminProfile();
        p.setPwHash(null); // 해시 클라이언트에 미노출
        return ResponseEntity.ok(p);
    }

    /* ── 프로필 수정 ── */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();

        AdminProfile p = new AdminProfile();
        p.setDisplayName(body.get("displayName"));
        p.setEmpnoDisplay(body.get("empnoDisplay"));
        String deptnoStr = body.get("deptno");
        if (deptnoStr != null && !deptnoStr.isBlank()) {
            try { p.setDeptno(Integer.parseInt(deptnoStr)); }
            catch (NumberFormatException ignored) {}
        }

        // 비밀번호 변경 (있는 경우만)
        String newPw = body.get("newPassword");
        if (newPw != null && !newPw.isBlank()) {
            if (newPw.length() < 4)
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "비밀번호는 4자 이상이어야 합니다."));
            p.setPwHash(passwordEncoder.encode(newPw));
        }

        adminMapper.upsertProfile(p);
        return ResponseEntity.ok(Map.of("result", "ok"));
    }

    /* ── 대리 관리자 목록 ── */
    @GetMapping("/proxy")
    public ResponseEntity<List<Map<String, Object>>> getProxyAdmins(Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(adminMapper.findProxyAdmins());
    }

    /* ── 대리 관리자 추가 ── */
    @PostMapping("/proxy/{empno}")
    public ResponseEntity<?> addProxy(
            @PathVariable Integer empno,
            Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        Emp emp = empMapper.findById(empno);
        if (emp == null)
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "존재하지 않는 사번입니다."));
        adminMapper.addProxyAdmin(empno);
        return ResponseEntity.ok(Map.of("result", "ok", "ename", emp.getEname()));
    }

    /* ── 대리 관리자 제거 ── */
    @DeleteMapping("/proxy/{empno}")
    public ResponseEntity<?> removeProxy(
            @PathVariable Integer empno,
            Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        adminMapper.removeProxyAdmin(empno);
        return ResponseEntity.ok(Map.of("result", "ok"));
    }
}
