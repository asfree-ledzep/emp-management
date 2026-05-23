package com.example.emp.controller;

import com.example.emp.model.Emp;
import com.example.emp.model.EmpAddr;
import com.example.emp.service.EmpAddrService;
import com.example.emp.service.EmpService;
import com.example.emp.service.ExcelService;
import com.example.emp.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emps")
public class EmpController {

    @Autowired private EmpService     empService;
    @Autowired private S3Service      s3Service;
    @Autowired private ExcelService   excelService;
    @Autowired private EmpAddrService empAddrService;

    // ── 권한 헬퍼 ──────────────────────────────────────────────
    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /** 로그인된 사원의 사번 (USER 역할일 때만 유효) */
    private Integer loginEmpno() {
        try {
            return Integer.parseInt(
                    SecurityContextHolder.getContext().getAuthentication().getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ── 전체 조회: 관리자만 ─────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> getAll() {
        if (!isAdmin()) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(empService.getAll());
    }

    // ── 단건 조회: 관리자 or 본인 ───────────────────────────────
    @GetMapping("/{empno}")
    public ResponseEntity<Emp> getById(@PathVariable Integer empno) {
        if (!isAdmin() && !empno.equals(loginEmpno())) {
            return ResponseEntity.status(403).build();
        }
        Emp emp = empService.getById(empno);
        if (emp == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(emp);
    }

    // ── 등록: 관리자만 ─────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Emp emp) {
        if (!isAdmin()) return ResponseEntity.status(403).build();
        try {
            empService.create(emp);
            return ResponseEntity.ok().build();
        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "이미 사용 중인 사번입니다. 다른 사번을 입력해 주세요."));
        }
    }

    // ── 수정: 관리자 or 본인 ────────────────────────────────────
    @PutMapping("/{empno}")
    public ResponseEntity<Void> update(@PathVariable Integer empno,
                                       @RequestBody Emp emp) {
        if (!isAdmin() && !empno.equals(loginEmpno())) {
            return ResponseEntity.status(403).build();
        }
        emp.setEmpno(empno);
        empService.update(emp);
        return ResponseEntity.ok().build();
    }

    // ── 삭제: 관리자만 ─────────────────────────────────────────
    @DeleteMapping("/{empno}")
    public ResponseEntity<Void> delete(@PathVariable Integer empno) {
        if (!isAdmin()) return ResponseEntity.status(403).build();
        empService.delete(empno);
        return ResponseEntity.ok().build();
    }

    // ── 부서별 조회: 관리자만 ───────────────────────────────────
    @GetMapping("/dept/{deptno}")
    public ResponseEntity<List<Emp>> getByDeptno(@PathVariable Integer deptno) {
        if (!isAdmin()) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(empService.getByDeptno(deptno));
    }

    // ── 엑셀 내보내기: 관리자만 ─────────────────────────────────
    @GetMapping("/export")
    public ResponseEntity<Map<String, String>> exportExcel() {
        if (!isAdmin()) return ResponseEntity.status(403).build();
        try {
            String url = excelService.exportToS3();
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── 엑셀 업로드 → 사원 일괄 등록: 관리자만 ─────────────────
    @PostMapping("/import")
    public ResponseEntity<?> importExcel(@RequestParam("file") MultipartFile file) {
        if (!isAdmin()) return ResponseEntity.status(403).build();
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "파일이 비어있습니다."));
        try {
            Map<String, Object> result = excelService.importFromExcel(file);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "파일 처리 실패: " + e.getMessage()));
        }
    }

    // ── 주소 조회: 관리자 or 본인 ───────────────────────────────
    @GetMapping("/{empno}/addr")
    public ResponseEntity<EmpAddr> getAddr(@PathVariable Integer empno) {
        if (!isAdmin() && !empno.equals(loginEmpno())) return ResponseEntity.status(403).build();
        EmpAddr addr = empAddrService.getByEmpno(empno);
        if (addr == null) addr = EmpAddr.builder().empno(empno).zipcode("").address("").addrDetail("").build();
        return ResponseEntity.ok(addr);
    }

    // ── 주소 저장: 관리자 or 본인 ───────────────────────────────
    @PutMapping("/{empno}/addr")
    public ResponseEntity<Void> saveAddr(@PathVariable Integer empno,
                                         @RequestBody EmpAddr addr) {
        if (!isAdmin() && !empno.equals(loginEmpno())) return ResponseEntity.status(403).build();
        addr.setEmpno(empno);
        empAddrService.save(addr);
        return ResponseEntity.ok().build();
    }

    // ── 사진 업로드: 관리자 or 본인 ─────────────────────────────
    @PostMapping("/{empno}/photo")
    public ResponseEntity<Map<String, String>> uploadPhoto(
            @PathVariable Integer empno,
            @RequestParam("file") MultipartFile file) {
        if (!isAdmin() && !empno.equals(loginEmpno())) {
            return ResponseEntity.status(403).build();
        }
        try {
            String url = s3Service.uploadPhoto(empno, file);
            empService.updatePhotoUrl(empno, url);
            return ResponseEntity.ok(Map.of("photoUrl", url));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
