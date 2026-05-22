package com.example.emp.service;

import com.example.emp.mapper.EmpKakaoMapper;
import com.example.emp.model.EmpKakao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class KakaoService {

    private static final Logger log = LoggerFactory.getLogger(KakaoService.class);

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Autowired
    private EmpKakaoMapper empKakaoMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // 카카오 인증 URL 반환
    public String getAuthUrl() {
        return "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=talk_message";
    }

    // 코드로 토큰 교환 후 DB 저장
    public void connectKakao(String code, Integer empno) throws Exception {
        String body = "grant_type=authorization_code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&code=" + code;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://kauth.kakao.com/oauth/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = objectMapper.readTree(response.body());

        if (!json.has("access_token")) {
            throw new RuntimeException("카카오 토큰 발급 실패: " + response.body());
        }

        String accessToken  = json.get("access_token").asText();
        String refreshToken = json.has("refresh_token") ? json.get("refresh_token").asText() : null;

        EmpKakao empKakao = new EmpKakao();
        empKakao.setEmpno(empno);
        empKakao.setAccessToken(accessToken);
        empKakao.setRefreshToken(refreshToken);
        empKakaoMapper.upsert(empKakao);

        log.info("카카오 연동 완료: empno={}", empno);
    }

    // 전체 사원에게 카카오톡 메시지 발송
    public void sendMessageToAll(String title, String content) {
        List<EmpKakao> kakaoList = empKakaoMapper.findAll();
        if (kakaoList.isEmpty()) {
            log.info("카카오 연동된 사원 없음");
            return;
        }
        for (EmpKakao empKakao : kakaoList) {
            try {
                sendMessage(empKakao, title, content);
            } catch (Exception e) {
                log.warn("카카오 메시지 실패 [empno={}], 토큰 갱신 시도: {}", empKakao.getEmpno(), e.getMessage());
                try {
                    String newToken = refreshAccessToken(empKakao);
                    if (newToken != null) {
                        empKakao.setAccessToken(newToken);
                        sendMessage(empKakao, title, content);
                    }
                } catch (Exception ex) {
                    log.warn("카카오 메시지 최종 실패 [empno={}]: {}", empKakao.getEmpno(), ex.getMessage());
                }
            }
        }
    }

    private void sendMessage(EmpKakao empKakao, String title, String content) throws Exception {
        Map<String, Object> template = Map.of(
                "object_type", "text",
                "text", "📢 [공지사항]\n\n" + title + "\n\n" + content,
                "link", Map.of(
                        "web_url", "https://emp-management-react.vercel.app",
                        "mobile_web_url", "https://emp-management-react.vercel.app"
                )
        );

        String templateJson = objectMapper.writeValueAsString(template);
        String requestBody = "template_object=" + URLEncoder.encode(templateJson, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://kapi.kakao.com/v2/api/talk/memo/default/send"))
                .header("Authorization", "Bearer " + empKakao.getAccessToken())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("상태코드: " + response.statusCode() + ", 응답: " + response.body());
        }
        log.info("카카오 메시지 발송 성공: empno={}", empKakao.getEmpno());
    }

    private String refreshAccessToken(EmpKakao empKakao) throws Exception {
        if (empKakao.getRefreshToken() == null) return null;

        String body = "grant_type=refresh_token"
                + "&client_id=" + clientId
                + "&refresh_token=" + empKakao.getRefreshToken();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://kauth.kakao.com/oauth/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = objectMapper.readTree(response.body());

        if (!json.has("access_token")) return null;

        String newAccessToken = json.get("access_token").asText();
        empKakaoMapper.updateAccessToken(empKakao.getEmpno(), newAccessToken);
        return newAccessToken;
    }
}
