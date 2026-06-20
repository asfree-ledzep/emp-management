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

/**
 * 에어코리아 실시간 미세먼지 API 프록시
 *
 * GET /api/dust?sido=서울
 *   → 해당 시도의 첫 번째 측정소 PM10·PM2.5·통합대기환경지수(KHAI) 반환
 *
 * 등급 기준 (1:좋음 2:보통 3:나쁨 4:매우나쁨)
 *   PM10  : 0~30 / 31~80 / 81~150 / 151~
 *   PM2.5 : 0~15 / 16~35 / 36~75  / 76~
 */
@RestController
@RequestMapping("/api/dust")
public class DustController {

    @Value("${dust.api.key:}")
    private String apiKey;

    private static final String BASE_URL =
            "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty";

    @GetMapping
    public ResponseEntity<?> getDust(
            @RequestParam(defaultValue = "서울") String sido) {

        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.ok(Map.of("error", "API_KEY_NOT_SET"));
        }

        try {
            String encodedKey  = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            String encodedSido = URLEncoder.encode(sido, StandardCharsets.UTF_8);

            String url = BASE_URL
                    + "?serviceKey=" + encodedKey
                    + "&returnType=json"
                    + "&numOfRows=5"
                    + "&pageNo=1"
                    + "&sidoName=" + encodedSido
                    + "&ver=1.0";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            String respBody = resp.body();

            // Forbidden / 비JSON 응답 처리
            if (resp.statusCode() == 403 || (respBody != null && respBody.trim().startsWith("Forbidden"))) {
                return ResponseEntity.status(403).body(Map.of(
                        "error", "API_FORBIDDEN",
                        "message", "공공데이터포털에서 '한국환경공단_에어코리아_대기오염정보' API 신청 필요"
                ));
            }
            if (respBody == null || (!respBody.trim().startsWith("{") && !respBody.trim().startsWith("["))) {
                return ResponseEntity.status(502).body(Map.of(
                        "error", "API_INVALID_RESPONSE",
                        "message", "에어코리아 API 응답 오류 (HTTP " + resp.statusCode() + ")"
                ));
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(respBody);

            // 에러 코드 확인
            String resultCode = root.path("response").path("header").path("resultCode").asText("");
            if (!resultCode.isEmpty() && !"00".equals(resultCode)) {
                String resultMsg = root.path("response").path("header").path("resultMsg").asText("API 오류");
                return ResponseEntity.status(502).body(Map.of(
                        "error", "API_ERROR_" + resultCode,
                        "message", "에어코리아: " + resultMsg
                ));
            }

            JsonNode items = root.path("response").path("body").path("items");

            if (!items.isArray() || items.isEmpty()) {
                return ResponseEntity.status(502).body(Map.of(
                        "error", "에어코리아 API 응답 없음",
                        "sido", sido
                ));
            }

            // 첫 번째 측정소 데이터 사용
            JsonNode item = items.get(0);

            String pm10Str  = item.path("pm10Value").asText("-");
            String pm25Str  = item.path("pm25Value").asText("-");
            String khaiStr  = item.path("khaiValue").asText("-");
            int    pm10Grade  = parseGrade(item.path("pm10Grade").asText("0"));
            int    pm25Grade  = parseGrade(item.path("pm25Grade").asText("0"));
            int    khaiGrade  = parseGrade(item.path("khaiGrade").asText("0"));
            String dataTime   = item.path("dataTime").asText("-");
            String stationName= item.path("stationName").asText(sido);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("sido",        sido);
            result.put("station",     stationName);
            result.put("dataTime",    dataTime);

            // PM10 미세먼지
            result.put("pm10",        parseIntSafe(pm10Str));
            result.put("pm10Grade",   pm10Grade);
            result.put("pm10Label",   gradeLabel(pm10Grade));
            result.put("pm10Color",   gradeColor(pm10Grade));
            result.put("pm10Emoji",   gradeEmoji(pm10Grade));

            // PM2.5 초미세먼지
            result.put("pm25",        parseIntSafe(pm25Str));
            result.put("pm25Grade",   pm25Grade);
            result.put("pm25Label",   gradeLabel(pm25Grade));
            result.put("pm25Color",   gradeColor(pm25Grade));
            result.put("pm25Emoji",   gradeEmoji(pm25Grade));

            // 통합대기환경지수 (KHAI)
            result.put("khai",        parseIntSafe(khaiStr));
            result.put("khaiGrade",   khaiGrade);
            result.put("khaiLabel",   gradeLabel(khaiGrade));
            result.put("khaiColor",   gradeColor(khaiGrade));

            // 재택 추천 여부 (나쁨 이상이면 권고)
            boolean wfh = pm10Grade >= 3 || pm25Grade >= 3;
            List<String> wfhReasons = new ArrayList<>();
            if (pm10Grade >= 3)  wfhReasons.add("미세먼지 " + gradeLabel(pm10Grade));
            if (pm25Grade >= 3)  wfhReasons.add("초미세먼지 " + gradeLabel(pm25Grade));
            result.put("wfh",         wfh);
            result.put("wfhReasons",  wfhReasons);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "미세먼지 조회 실패: " + e.getMessage()));
        }
    }

    // ── 헬퍼 ──────────────────────────────────────────
    private int parseGrade(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return -1; }
    }

    private String gradeLabel(int grade) {
        return switch (grade) {
            case 1 -> "좋음";
            case 2 -> "보통";
            case 3 -> "나쁨";
            case 4 -> "매우나쁨";
            default -> "-";
        };
    }

    private String gradeColor(int grade) {
        return switch (grade) {
            case 1 -> "#3b82f6";   // 파란색 - 좋음
            case 2 -> "#22c55e";   // 초록색 - 보통
            case 3 -> "#f97316";   // 주황색 - 나쁨
            case 4 -> "#ef4444";   // 빨간색 - 매우나쁨
            default -> "#94a3b8";  // 회색 - 알 수 없음
        };
    }

    private String gradeEmoji(int grade) {
        return switch (grade) {
            case 1 -> "😊";
            case 2 -> "🙂";
            case 3 -> "😷";
            case 4 -> "🚨";
            default -> "❓";
        };
    }
}
