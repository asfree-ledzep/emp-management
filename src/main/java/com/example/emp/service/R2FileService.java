package com.example.emp.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Cloudflare R2 파일 스토리지 서비스 (AWS SDK v2, S3-호환 API 사용)
 */
@Service
public class R2FileService {

    @Value("${r2.account-id:}")
    private String accountId;

    @Value("${r2.access-key:}")
    private String accessKey;

    @Value("${r2.secret-key:}")
    private String secretKey;

    @Value("${r2.bucket:emp-shared-files}")
    private String bucket;

    private S3Client client;
    private S3Presigner presigner;
    private boolean ready = false;

    @PostConstruct
    public void init() {
        if (accountId == null || accountId.isBlank() ||
            accessKey == null || accessKey.isBlank() ||
            secretKey == null || secretKey.isBlank()) {
            return;
        }
        var endpoint    = URI.create("https://" + accountId + ".r2.cloudflarestorage.com");
        var region      = Region.of("auto");
        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey));
        var s3Config = S3Configuration.builder().pathStyleAccessEnabled(true).build();

        client = S3Client.builder()
                .endpointOverride(endpoint).region(region)
                .credentialsProvider(credentials)
                .serviceConfiguration(s3Config).build();

        presigner = S3Presigner.builder()
                .endpointOverride(endpoint).region(region)
                .credentialsProvider(credentials)
                .serviceConfiguration(s3Config).build();

        ready = true;
    }

    public boolean isReady() { return ready; }

    /** 파일 업로드 → R2 key 반환 */
    public String upload(String key, MultipartFile file) throws IOException {
        checkReady();
        client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
        return key;
    }

    /** 파일 스트리밍 다운로드 (OutputStream으로 직접 전달) */
    public void streamTo(String key, OutputStream out) throws IOException {
        checkReady();
        try (var is = client.getObject(GetObjectRequest.builder()
                .bucket(bucket).key(key).build())) {
            is.transferTo(out);
        }
    }

    /** Pre-signed 다운로드 URL 생성 (10분 유효) */
    public String generatePresignedUrl(String key, String fileName) {
        checkReady();
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        var getReq = GetObjectRequest.builder()
                .bucket(bucket).key(key)
                .responseContentDisposition("attachment; filename*=UTF-8''" + encoded)
                .build();
        var presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getReq).build();
        return presigner.presignGetObject(presignReq).url().toString();
    }

    /** 파일 삭제 */
    public void delete(String key) {
        checkReady();
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    private void checkReady() {
        if (!ready) throw new IllegalStateException("R2 자격증명이 설정되지 않았습니다. EB 환경변수를 확인하세요.");
    }
}
