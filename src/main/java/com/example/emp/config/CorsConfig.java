package com.example.emp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS(Cross-Origin Resource Sharing) 전역 설정 클래스
 * React 개발서버(http://localhost:3000)에서 오는 API 요청을 허용한다.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry
            // 모든 API 경로에 대해 CORS 정책 적용
            .addMapping("/api/**")

            // React 개발서버 및 Vercel 배포 출처 허용
            .allowedOrigins("http://localhost:3000", "https://emp-management-react.vercel.app")

            // 허용할 HTTP 메서드 (조회·등록·수정·삭제 전부 포함)
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")

            // 모든 요청 헤더 허용
            .allowedHeaders("*")

            // 자격증명(쿠키, Authorization 헤더 등) 포함 요청 허용
            .allowCredentials(true)

            // preflight 요청 결과 캐시 시간 (초 단위: 1시간)
            .maxAge(3600);
    }
}
