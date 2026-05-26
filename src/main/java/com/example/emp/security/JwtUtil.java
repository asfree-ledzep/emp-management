package com.example.emp.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // 토큰 생성 (subject: admin아이디 또는 사번 문자열, role: ADMIN|USER)
    public String generateToken(String subject, String role) {
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    // 토큰에서 subject(아이디/사번) 추출
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    // 토큰에서 role 추출
    public String extractRole(String token) {
        return (String) getClaims(token).get("role");
    }

    // 토큰 유효성 검사 (서명 + 만료)
    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("[JWT] 토큰 검증 실패: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    private io.jsonwebtoken.Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
