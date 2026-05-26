package com.example.emp.controller;

import com.example.emp.mapper.DeptMapper;
import com.example.emp.mapper.EmpAuthMapper;
import com.example.emp.mapper.EmpMapper;
import com.example.emp.model.Dept;
import com.example.emp.model.EmpAuth;
import com.example.emp.model.Emp;
import com.example.emp.model.LoginRequest;
import com.example.emp.model.SetPasswordRequest;
import com.example.emp.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Autowired private JwtUtil        jwtUtil;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmpAuthMapper  empAuthMapper;
    @Autowired private EmpMapper      empMapper;
    @Autowired private DeptMapper     deptMapper;

    // POST /api/auth/login
    // 관리자: 아이디/비밀번호
    // 일반사원: 사번(숫자)/개인 비밀번호
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        String id = req.getUsername();
        String pw = req.getPassword();

        // 1. 관리자 계정 확인
        if (adminUsername.equals(id) && adminPassword.equals(pw)) {
            String token = jwtUtil.generateToken(id, "ADMIN");
            return ResponseEntity.ok(Map.of(
                    "token",    token,
                    "username", id,
                    "role",     "ADMIN"
            ));
        }

        // 2. 사원 계정 확인 (사번으로 로그인)
        try {
            Integer empno   = Integer.parseInt(id);
            EmpAuth empAuth = empAuthMapper.findByEmpno(empno);
            if (empAuth != null && passwordEncoder.matches(pw, empAuth.getPassword())) {
                Emp emp     = empMapper.findById(empno);
                String name = (emp != null && emp.getEname() != null) ? emp.getEname() : id;
                String token = jwtUtil.generateToken(id, "USER");
                java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
                body.put("token",    token);
                body.put("username", name);
                body.put("role",     "USER");
                body.put("empno",    empno);
                if (emp != null && emp.getDeptno() != null) {
                    body.put("deptno", emp.getDeptno());
                    Dept dept = deptMapper.findById(emp.getDeptno());
                    if (dept != null && dept.getDname() != null) {
                        body.put("deptname", dept.getDname());
                    }
                }
                return ResponseEntity.ok(body);
            }
        } catch (NumberFormatException ignored) {}

        return ResponseEntity.status(401).body(Map.of(
                "message", "아이디 또는 비밀번호가 틀렸습니다."
        ));
    }

    // POST /api/auth/set-password  (관리자 전용)
    // 사원의 초기 비밀번호 등록 또는 재설정
    @PostMapping("/set-password")
    public ResponseEntity<?> setPassword(@RequestBody SetPasswordRequest req) {
        if (!isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("message", "관리자만 사용 가능합니다."));
        }
        if (req.getPassword() == null || req.getPassword().length() < 4) {
            return ResponseEntity.badRequest().body(Map.of("message", "비밀번호는 4자 이상이어야 합니다."));
        }
        EmpAuth empAuth = new EmpAuth();
        empAuth.setEmpno(req.getEmpno());
        empAuth.setPassword(passwordEncoder.encode(req.getPassword()));
        empAuthMapper.upsert(empAuth);
        return ResponseEntity.ok().build();
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
