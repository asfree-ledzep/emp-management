package com.example.emp.service;

import com.example.emp.mapper.QrTokenMapper;
import com.example.emp.model.QrToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class QrService {

    private final QrTokenMapper qrTokenMapper;

    public QrService(QrTokenMapper qrTokenMapper) {
        this.qrTokenMapper = qrTokenMapper;
    }

    /**
     * 현재 유효한 QR 토큰 반환.
     * 만료됐거나 없으면 새로 생성.
     */
    @Transactional
    public QrToken getCurrentToken() {
        QrToken token = qrTokenMapper.findValidToken();
        if (token == null) {
            qrTokenMapper.deleteExpired();
            QrToken newToken = new QrToken();
            newToken.setToken(UUID.randomUUID().toString());
            qrTokenMapper.insert(newToken);
            token = qrTokenMapper.findValidToken();
        }
        return token;
    }

    /**
     * 강제 새 토큰 발급 (관리자 🔄 버튼).
     * 기존 토큰 전부 삭제 후 새 5분 토큰 생성.
     */
    @Transactional
    public QrToken forceGenerateToken() {
        qrTokenMapper.deleteAll();
        QrToken newToken = new QrToken();
        newToken.setToken(UUID.randomUUID().toString());
        qrTokenMapper.insert(newToken);
        return qrTokenMapper.findValidToken();
    }

    /**
     * 토큰 유효성 검증 (스캔 시 호출).
     */
    public QrToken validateToken(String token) {
        return qrTokenMapper.findByToken(token);
    }
}
