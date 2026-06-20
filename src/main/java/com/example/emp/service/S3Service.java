package com.example.emp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final String bucket;
    private final String region;

    public S3Service(@Value("${s3.bucket}") String bucket,
                     @Value("${s3.region}") String region) {
        this.bucket = bucket;
        this.region = region;
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    public String uploadPhoto(Integer empno, MultipartFile file) throws IOException {
        String ext = getExtension(file.getOriginalFilename());
        String key = "photos/emp-" + empno + "." + ext;

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    // Excel 등 바이트 배열 파일 업로드
    public String uploadBytes(String key, byte[] data, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(data)
        );
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    // 영수증 이미지 업로드 (receipts/emp-{empno}/{timestamp}.{ext})
    public String uploadReceipt(Integer empno, MultipartFile file) throws IOException {
        String ext = getExtension(file.getOriginalFilename());
        String key = "receipts/emp-" + empno + "/" + System.currentTimeMillis() + "." + ext;

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    // 게시판 첨부파일 업로드 (board/emp-{empno}/{timestamp}_{safeFilename})
    public String uploadBoardFile(Integer empno, MultipartFile file) throws IOException {
        String original     = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String safeFilename = original.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        String key          = "board/emp-" + empno + "/" + System.currentTimeMillis() + "_" + safeFilename;
        String encodedName  = URLEncoder.encode(original, StandardCharsets.UTF_8).replace("+", "%20");
        String contentDisp  = "attachment; filename=\"" + safeFilename + "\"; filename*=UTF-8''" + encodedName;

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket).key(key)
                        .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                        .contentDisposition(contentDisp)
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    // 채팅 첨부파일 업로드 (chat/emp-{empno}/{timestamp}_{safeFilename})
    public String uploadChatFile(Integer empno, MultipartFile file) throws IOException {
        String original    = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        // S3 키: 한글·공백·특수문자 → '_' 치환 (URL 안전)
        String safeFilename = original.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        String key          = "chat/emp-" + empno + "/" + System.currentTimeMillis() + "_" + safeFilename;

        // Content-Disposition: 브라우저가 다운로드로 처리하도록 강제
        // RFC 5987: filename*=UTF-8''... 으로 한글 원본 파일명을 다운로드 다이얼로그에 표시
        String encodedName  = URLEncoder.encode(original, StandardCharsets.UTF_8).replace("+", "%20");
        String contentDisp  = "attachment; filename=\"" + safeFilename + "\"; filename*=UTF-8''" + encodedName;

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                        .contentDisposition(contentDisp)
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    // 쪽지 첨부파일 업로드 (msg/emp-{empno}/{timestamp}_{safeFilename})
    public String uploadMsgFile(Integer empno, MultipartFile file) throws IOException {
        String original     = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String safeFilename = original.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        String key          = "msg/emp-" + empno + "/" + System.currentTimeMillis() + "_" + safeFilename;
        String encodedName  = URLEncoder.encode(original, StandardCharsets.UTF_8).replace("+", "%20");
        String contentDisp  = "attachment; filename=\"" + safeFilename + "\"; filename*=UTF-8''" + encodedName;

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket).key(key)
                        .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                        .contentDisposition(contentDisp)
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    // S3 사진 삭제 (URL에서 키 추출)
    public void deletePhoto(String photoUrl) {
        try {
            String key = photoUrl.substring(photoUrl.indexOf("amazonaws.com/") + "amazonaws.com/".length());
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (Exception e) {
            // 삭제 실패해도 사원 삭제에는 영향 없음
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
