package com.example.emp.controller;

import com.example.emp.mapper.BoardMapper;
import com.example.emp.model.BoardPost;
import com.example.emp.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 게시판 컨트롤러
 *
 * [전체게시판]  GET  /api/board/global        — 목록 조회
 *              POST /api/board/global        — 글 작성
 *              DELETE /api/board/global/{id} — 본인 글 삭제
 *
 * [부서게시판]  GET  /api/board/dept?deptno=X — 목록 조회
 *              POST /api/board/dept          — 글 작성
 *              DELETE /api/board/dept/{id}   — 본인 글 삭제
 *
 * [공통]       POST /api/board/upload        — 파일 S3 업로드
 *              GET  /api/board/download?url= — 파일 다운로드 프록시
 */
@RestController
@RequestMapping("/api/board")
public class BoardController {

    @Autowired private BoardMapper boardMapper;
    @Autowired private S3Service   s3Service;

    /* ── 공통: 현재 사용자 empno 추출 ── */
    private Integer empno(Authentication auth) {
        if (auth == null) return null;
        try { return Integer.parseInt(auth.getName()); }
        catch (NumberFormatException e) { return null; }
    }

    /* ── 공통: 현재 사용자 이름 추출 ── */
    private String authorName(Authentication auth) {
        if (auth == null) return "알 수 없음";
        return auth.getName();
    }

    // ── 전체게시판 목록 ──────────────────────────────────────
    @GetMapping("/global")
    public ResponseEntity<List<BoardPost>> globalList() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(boardMapper.findGlobal());
    }

    // ── 전체게시판 글 작성 ────────────────────────────────────
    @PostMapping("/global")
    public ResponseEntity<?> globalWrite(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        Integer empno = empno(auth);
        if (empno == null) return ResponseEntity.status(401).build();

        BoardPost post = new BoardPost();
        post.setEmpno(empno);
        post.setAuthorName(body.getOrDefault("authorName", "알 수 없음"));
        post.setTitle(body.get("title"));
        post.setContent(body.get("content"));
        post.setFileUrl(body.get("fileUrl"));
        post.setFileName(body.get("fileName"));
        boardMapper.insertGlobal(post);
        return ResponseEntity.ok(Map.of("result", "ok"));
    }

    // ── 전체게시판 글 삭제 (본인만) ──────────────────────────
    @DeleteMapping("/global/{id}")
    public ResponseEntity<?> globalDelete(
            @PathVariable long id,
            Authentication auth) {
        Integer empno = empno(auth);
        if (empno == null) return ResponseEntity.status(401).build();
        int affected = boardMapper.deleteByIdAndEmpno(id, empno);
        return affected > 0
                ? ResponseEntity.ok(Map.of("result", "ok"))
                : ResponseEntity.status(403).body(Map.of("message", "삭제 권한이 없습니다."));
    }

    // ── 부서게시판 목록 ──────────────────────────────────────
    @GetMapping("/dept")
    public ResponseEntity<List<BoardPost>> deptList(@RequestParam int deptno) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(boardMapper.findByDept(deptno));
    }

    // ── 부서게시판 글 작성 ────────────────────────────────────
    @PostMapping("/dept")
    public ResponseEntity<?> deptWrite(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        Integer empno = empno(auth);
        if (empno == null) return ResponseEntity.status(401).build();

        String deptnoStr = body.get("deptno");
        if (deptnoStr == null) return ResponseEntity.badRequest().body(Map.of("message", "deptno 필요"));
        Integer deptno;
        try { deptno = Integer.parseInt(deptnoStr); }
        catch (NumberFormatException e) { return ResponseEntity.badRequest().build(); }

        BoardPost post = new BoardPost();
        post.setEmpno(empno);
        post.setDeptno(deptno);
        post.setAuthorName(body.getOrDefault("authorName", "알 수 없음"));
        post.setTitle(body.get("title"));
        post.setContent(body.get("content"));
        post.setFileUrl(body.get("fileUrl"));
        post.setFileName(body.get("fileName"));
        boardMapper.insertDept(post);
        return ResponseEntity.ok(Map.of("result", "ok"));
    }

    // ── 부서게시판 글 삭제 (본인만) ──────────────────────────
    @DeleteMapping("/dept/{id}")
    public ResponseEntity<?> deptDelete(
            @PathVariable long id,
            Authentication auth) {
        Integer empno = empno(auth);
        if (empno == null) return ResponseEntity.status(401).build();
        int affected = boardMapper.deleteByIdAndEmpno(id, empno);
        return affected > 0
                ? ResponseEntity.ok(Map.of("result", "ok"))
                : ResponseEntity.status(403).body(Map.of("message", "삭제 권한이 없습니다."));
    }

    // ── 파일 업로드 → S3 → { url, fileName } ────────────────
    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        try {
            Integer empno = 0;
            try { empno = Integer.parseInt(auth.getName()); } catch (NumberFormatException ignored) {}
            String url = s3Service.uploadBoardFile(empno, file);
            return ResponseEntity.ok(Map.of(
                    "url",      url,
                    "fileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "파일 업로드 실패: " + e.getMessage()));
        }
    }

    // ── 파일 다운로드 프록시 (board/ 경로만 허용) ─────────────
    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam String url) {
        try {
            URI uri = new URI(url);
            if (!uri.getPath().contains("/board/")) return ResponseEntity.badRequest().build();
            try (InputStream in = uri.toURL().openStream()) {
                byte[] data    = in.readAllBytes();
                String path    = uri.getPath();
                String rawName = path.substring(path.lastIndexOf('/') + 1);
                String fileName = rawName.contains("_") ? rawName.substring(rawName.indexOf('_') + 1) : rawName;
                String encoded  = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encoded)
                        .header(HttpHeaders.CACHE_CONTROL, "no-store")
                        .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                        .body(data);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
