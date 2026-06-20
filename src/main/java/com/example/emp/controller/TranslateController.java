package com.example.emp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/**
 * NCP 파파고 번역 API 프록시
 *
 * POST /api/translate
 *   body: { "text": "번역할 텍스트", "target": "en" | "zh-CN" | "ja" }
 *   → { "translatedText": "..." }
 *
 * NCP Papago Text Translation (월 50,000자 무료)
 * 환경변수: PAPAGO_CLIENT_ID, PAPAGO_CLIENT_SECRET
 */
@RestController
@RequestMapping("/api/translate")
public class TranslateController {

    @Value("${papago.client-id:}")
    private String clientId;

    @Value("${papago.client-secret:}")
    private String clientSecret;

    private static final String PAPAGO_URL =
            "https://papago.apigw.ntruss.com/nmt/v1/translation";

    private static final Set<String> ALLOWED_TARGETS =
            Set.of("en", "zh-CN", "zh-TW", "ja", "fr", "de", "es", "vi", "th", "id");

    @PostMapping
    public ResponseEntity<?> translate(@RequestBody Map<String, String> body) {

        if (clientId == null || clientId.isBlank()) {
            return ResponseEntity.ok(Map.of(
                    "error",   "API_KEY_NOT_SET",
                    "message", "PAPAGO_CLIENT_ID 환경변수 미설정"
            ));
        }

        String text   = body.getOrDefault("text", "").trim();
        String target = body.getOrDefault("target", "en").trim();

        if (text.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "텍스트가 비어 있습니다."));
        }
        if (!ALLOWED_TARGETS.contains(target)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "지원하지 않는 언어: " + target));
        }

        // 요청당 최대 5,000자
        if (text.length() > 5000) {
            text = text.substring(0, 5000);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(Map.of(
                    "source", "ko",
                    "target", target,
                    "text",   text
            ));

            HttpClient  client  = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PAPAGO_URL))
                    .header("Content-Type",          "application/json; charset=UTF-8")
                    .header("x-ncp-apigw-api-key-id", clientId)
                    .header("x-ncp-apigw-api-key",    clientSecret)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 401 || resp.statusCode() == 403) {
                return ResponseEntity.ok(Map.of(
                        "error",   "API_FORBIDDEN",
                        "message", "파파고 API 인증 실패 — NCP Client ID/Secret 확인 필요"
                ));
            }

            JsonNode root = mapper.readTree(resp.body());

            // 오류 응답
            if (root.has("error")) {
                return ResponseEntity.ok(Map.of(
                        "error",   "PAPAGO_ERROR",
                        "message", root.path("error").path("message").asText("번역 오류")
                ));
            }

            String translated = root
                    .path("message")
                    .path("result")
                    .path("translatedText")
                    .asText("");

            if (translated.isBlank()) {
                return ResponseEntity.ok(Map.of("error", "번역 결과가 없습니다."));
            }

            return ResponseEntity.ok(Map.of("translatedText", translated));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "번역 요청 실패: " + e.getMessage()));
        }
    }
}
