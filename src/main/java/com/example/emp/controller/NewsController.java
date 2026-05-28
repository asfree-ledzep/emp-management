package com.example.emp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 네이버 뉴스 검색 API 프록시 (사원 전용)
 *
 * GET /api/news?query=검색어&display=5
 *   → 최신 뉴스 N건 반환
 *
 * 필요 환경변수:
 *   NAVER_CLIENT_ID, NAVER_CLIENT_SECRET
 *   (https://developers.naver.com 에서 "검색" API 신청)
 */
@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Value("${naver.client-id:}")
    private String clientId;

    @Value("${naver.client-secret:}")
    private String clientSecret;

    private static final String NEWS_API_URL =
            "https://openapi.naver.com/v1/search/news.json";

    // HTML 태그 제거용 패턴
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    @GetMapping
    public ResponseEntity<?> getNews(
            @RequestParam(defaultValue = "IT 뉴스") String query,
            @RequestParam(defaultValue = "5")  int    display) {

        if (clientId == null || clientId.isBlank()) {
            return ResponseEntity.ok(Map.of(
                    "error",   "API_KEY_NOT_SET",
                    "message", "NAVER_CLIENT_ID / NAVER_CLIENT_SECRET 환경변수 미설정"
            ));
        }

        try {
            int safeDisplay = Math.min(Math.max(display, 1), 10);
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

            String url = NEWS_API_URL
                    + "?query="   + encodedQuery
                    + "&display=" + safeDisplay
                    + "&start=1"
                    + "&sort=date";   // 최신순

            HttpClient  client  = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Naver-Client-Id",     clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .GET()
                    .build();

            HttpResponse<String> resp =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            // Naver API 오류는 항상 HTTP 200으로 감싸서 반환
            // (4xx를 그대로 전달하면 클라이언트가 JWT 인증 에러로 혼동함)
            if (resp.statusCode() == 401 || resp.statusCode() == 403) {
                return ResponseEntity.ok(Map.of(
                        "error",   "API_FORBIDDEN",
                        "message", "네이버 API 인증 실패 — Client ID/Secret 확인 필요"
                ));
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(resp.body());
            JsonNode items = root.path("items");

            List<Map<String, Object>> result = new ArrayList<>();
            if (items.isArray()) {
                for (JsonNode item : items) {
                    Map<String, Object> news = new LinkedHashMap<>();
                    news.put("title",       stripHtml(item.path("title").asText()));
                    news.put("link",        item.path("link").asText());
                    news.put("description", stripHtml(item.path("description").asText()));
                    news.put("pubDate",     item.path("pubDate").asText());
                    news.put("source",      extractSource(item.path("originallink").asText(
                                                         item.path("link").asText())));
                    result.add(news);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "query",      query,
                    "total",      root.path("total").asInt(0),
                    "items",      result
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "뉴스 조회 실패: " + e.getMessage()));
        }
    }

    // ── 헬퍼 ───────────────────────────────────────────────────

    /** HTML 태그 & 특수문자 엔티티 제거 */
    private String stripHtml(String raw) {
        if (raw == null) return "";
        return HTML_TAG.matcher(raw)
                .replaceAll("")
                .replace("&amp;",  "&")
                .replace("&lt;",   "<")
                .replace("&gt;",   ">")
                .replace("&quot;", "\"")
                .replace("&#39;",  "'")
                .trim();
    }

    /** URL에서 도메인(출처) 추출 */
    private String extractSource(String url) {
        if (url == null || url.isBlank()) return "";
        try {
            String host = URI.create(url).getHost();
            if (host == null) return "";
            // www. 제거
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return "";
        }
    }
}
