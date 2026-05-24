package com.example.emp.service;

import com.example.emp.mapper.FileShareMapper;
import com.example.emp.model.FileShare;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

@Service
public class FileShareService {

    @Autowired private FileShareMapper mapper;
    @Autowired private R2FileService   r2;

    // ── 업로드 ──
    public void upload(MultipartFile file, String scope, Integer deptno,
                       String uploader, String uploaderName) throws IOException {
        // R2 키: shared/ALL/{uuid}_{name} 또는 shared/DEPT/{deptno}/{uuid}_{name}
        String uuid     = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String safeName = file.getOriginalFilename()
                .replaceAll("[^a-zA-Z0-9._가-힣\\-]", "_");
        String key = "shared/" + scope
                + ("DEPT".equals(scope) && deptno != null ? "/" + deptno : "")
                + "/" + uuid + "_" + safeName;

        r2.upload(key, file);

        FileShare fs = new FileShare();
        fs.setFileName(file.getOriginalFilename());
        fs.setR2Key(key);
        fs.setFileSize(file.getSize());
        fs.setMimeType(file.getContentType());
        fs.setScope(scope);
        fs.setDeptno(deptno);
        fs.setUploader(uploader);
        fs.setUploaderName(uploaderName);
        mapper.insert(fs);
    }

    // ── 다운로드 스트리밍 ──
    public FileShare findById(Long fileId) {
        return mapper.findById(fileId);
    }

    public void streamFile(Long fileId, OutputStream out) throws IOException {
        FileShare fs = mapper.findById(fileId);
        if (fs == null) throw new IllegalArgumentException("파일이 없습니다.");
        r2.streamTo(fs.getR2Key(), out);
    }

    // ── 목록 조회 ──
    public List<FileShare> findByScope(String scope) {
        return mapper.findByScope(scope);
    }

    public List<FileShare> findByDeptno(Integer deptno) {
        return mapper.findByDeptno(deptno);
    }

    public List<FileShare> findForAdmin(String scope, Integer deptno) {
        return mapper.findAllAdmin(scope, deptno);
    }

    // ── 삭제 ──
    public void delete(Long fileId, String uploaderKey) {
        FileShare fs = mapper.findById(fileId);
        if (fs == null) throw new IllegalArgumentException("파일이 없습니다.");
        // uploaderKey = null 이면 ADMIN (모든 파일 삭제 가능)
        if (uploaderKey != null && !uploaderKey.equals(fs.getUploader())) {
            throw new IllegalArgumentException("본인이 올린 파일만 삭제할 수 있습니다.");
        }
        try { r2.delete(fs.getR2Key()); } catch (Exception ignored) {}
        mapper.deleteById(fileId);
    }

    public boolean isR2Ready() { return r2.isReady(); }
}
