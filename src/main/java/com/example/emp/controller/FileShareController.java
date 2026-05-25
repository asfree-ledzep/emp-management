package com.example.emp.controller;

import com.example.emp.mapper.EmpMapper;
import com.example.emp.model.Emp;
import com.example.emp.model.FileShare;
import com.example.emp.service.FileShareService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileShareController {

    @Autowired private FileShareService fileShareService;
    @Autowired private EmpMapper        empMapper;

    // ── R2 설정 상태 확인 ──
    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of("ready", fileShareService.isR2Ready());
    }

    // ── 사원: 목록 조회 ──
    // scope=ALL → 전체 공유 폴더
    // scope=DEPT → 본인 부서 폴더
    // folderId=N → 해당 폴더 내 파일 (null = 루트)
    @GetMapping
    public List<FileShare> list(@RequestParam String scope,
                                @RequestParam(required = false) Long folderId,
                                Authentication auth) {
        if ("ALL".equals(scope)) {
            return fileShareService.findByScope("ALL", folderId);
        }
        // DEPT: 본인 부서 자동 판별
        Integer empno = Integer.parseInt(auth.getName());
        Emp emp = empMapper.findById(empno);
        if (emp == null || emp.getDeptno() == null) return List.of();
        return fileShareService.findByDeptno(emp.getDeptno(), folderId);
    }

    // ── 관리자: 전체 목록 (scope/deptno/folderId 필터 가능) ──
    @GetMapping("/admin/all")
    public List<FileShare> adminAll(
            @RequestParam(required = false) String  scope,
            @RequestParam(required = false) Integer deptno,
            @RequestParam(required = false) Long    folderId,
            Authentication auth) {
        checkAdmin(auth);
        return fileShareService.findForAdmin(scope, deptno, folderId);
    }

    // ── 파일 업로드 ──
    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam MultipartFile              file,
            @RequestParam String                     scope,
            @RequestParam(required = false) Integer  deptno,
            @RequestParam(required = false) Long     folderId,
            Authentication auth) {

        if (!fileShareService.isR2Ready()) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "R2 자격증명이 설정되지 않았습니다."));
        }

        boolean isAdmin = isAdmin(auth);
        String uploader;
        String uploaderName;
        Integer actualDeptno = deptno;

        if (isAdmin) {
            uploader     = "admin";
            uploaderName = "관리자";
        } else {
            uploader = auth.getName();
            Emp emp  = empMapper.findById(Integer.parseInt(uploader));
            uploaderName = (emp != null) ? emp.getEname() : uploader;
            // DEPT 업로드이고 deptno 미지정 시 본인 부서 자동
            if ("DEPT".equals(scope) && actualDeptno == null && emp != null) {
                actualDeptno = emp.getDeptno();
            }
        }

        try {
            fileShareService.upload(file, scope, actualDeptno, uploader, uploaderName, folderId);
            return ResponseEntity.ok(Map.of("result", "ok"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "업로드 실패: " + e.getMessage()));
        }
    }

    // ── 파일 폴더 이동 ──
    @PatchMapping("/{id}/move")
    public ResponseEntity<?> moveToFolder(@PathVariable Long id,
                                          @RequestBody Map<String, Object> body) {
        Long folderId = body.get("folderId") != null
                ? Long.valueOf(body.get("folderId").toString()) : null;
        fileShareService.moveToFolder(id, folderId);
        return ResponseEntity.ok(Map.of("result", "ok"));
    }

    // ── 파일 다운로드 (서버 스트리밍) ──
    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, HttpServletResponse response,
                         Authentication auth) throws IOException {
        FileShare fs = fileShareService.findById(id);
        if (fs == null) { response.sendError(404); return; }

        // 부서 공유 파일: 본인 부서 또는 관리자만 다운로드
        if ("DEPT".equals(fs.getScope()) && !isAdmin(auth)) {
            Integer empno = Integer.parseInt(auth.getName());
            Emp emp = empMapper.findById(empno);
            if (emp == null || !fs.getDeptno().equals(emp.getDeptno())) {
                response.sendError(403, "접근 권한이 없습니다.");
                return;
            }
        }

        String mimeType = (fs.getMimeType() != null)
                ? fs.getMimeType() : "application/octet-stream";
        String encoded  = URLEncoder.encode(fs.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        response.setContentType(mimeType);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        if (fs.getFileSize() != null) response.setContentLengthLong(fs.getFileSize());

        fileShareService.streamFile(id, response.getOutputStream());
    }

    // ── 파일 삭제 ──
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        boolean admin = isAdmin(auth);
        fileShareService.delete(id, admin ? null : auth.getName());
        return ResponseEntity.ok(Map.of("result", "ok"));
    }

    // ── 유틸 ──
    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
    private void checkAdmin(Authentication auth) {
        if (!isAdmin(auth)) throw new RuntimeException("관리자 전용 기능입니다.");
    }
}
