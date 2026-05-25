package com.example.emp.controller;

import com.example.emp.mapper.EmpMapper;
import com.example.emp.model.Emp;
import com.example.emp.model.FileFolder;
import com.example.emp.service.FileFolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/folders")
public class FileFolderController {

    @Autowired private FileFolderService folderService;
    @Autowired private EmpMapper         empMapper;

    // ── 폴더 목록 조회 ──
    // parentId = null  → 루트 폴더
    // parentId = N     → 하위 폴더
    @GetMapping
    public List<FileFolder> list(
            @RequestParam String                 scope,
            @RequestParam(required = false) Long    parentId,
            @RequestParam(required = false) Integer deptno,
            Authentication auth) {

        // DEPT scope: deptno 자동 결정
        Integer actualDeptno = deptno;
        if ("DEPT".equals(scope) && actualDeptno == null && !isAdmin(auth)) {
            Integer empno = Integer.parseInt(auth.getName());
            Emp emp = empMapper.findById(empno);
            if (emp != null) actualDeptno = emp.getDeptno();
        }

        if (parentId == null) {
            return folderService.getRootFolders(scope, actualDeptno);
        } else {
            return folderService.getSubFolders(parentId, scope, actualDeptno);
        }
    }

    // ── 폴더 생성 ──
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body, Authentication auth) {
        String  folderName = (String)  body.get("folderName");
        String  scope      = (String)  body.get("scope");
        Long    parentId   = body.get("parentId") != null
                ? Long.valueOf(body.get("parentId").toString()) : null;
        Integer deptno     = body.get("deptno") != null
                ? Integer.valueOf(body.get("deptno").toString()) : null;

        if (folderName == null || folderName.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "폴더 이름을 입력하세요."));
        if (scope == null)
            return ResponseEntity.badRequest().body(Map.of("error", "scope 를 지정하세요."));

        boolean isAdmin = isAdmin(auth);
        String  creator;
        String  creatorName;

        if (isAdmin) {
            creator     = "admin";
            creatorName = "관리자";
        } else {
            creator = auth.getName();
            Emp emp = empMapper.findById(Integer.parseInt(creator));
            creatorName = (emp != null) ? emp.getEname() : creator;
            if ("DEPT".equals(scope) && deptno == null && emp != null) {
                deptno = emp.getDeptno();
            }
        }

        try {
            folderService.create(folderName, parentId, scope, deptno, creator, creatorName);
            return ResponseEntity.ok(Map.of("result", "ok"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── 폴더 이름 변경 ──
    @PutMapping("/{id}")
    public ResponseEntity<?> rename(@PathVariable Long id,
                                    @RequestBody Map<String, String> body,
                                    Authentication auth) {
        String newName = body.get("folderName");
        if (newName == null || newName.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "폴더 이름을 입력하세요."));
        try {
            folderService.rename(id, newName, auth.getName(), isAdmin(auth));
            return ResponseEntity.ok(Map.of("result", "ok"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // ── 폴더 삭제 ──
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        try {
            folderService.delete(id, auth.getName(), isAdmin(auth));
            return ResponseEntity.ok(Map.of("result", "ok"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
