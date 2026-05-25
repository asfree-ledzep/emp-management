package com.example.emp.mapper;

import com.example.emp.model.QrToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface QrTokenMapper {
    void insert(QrToken token);
    QrToken findValidToken();  // 현재 유효한 토큰
    QrToken findByToken(@Param("token") String token);
    void deleteExpired();      // 만료된 토큰 정리
    void deleteAll();          // 강제 재발급용 - 전체 삭제
}
