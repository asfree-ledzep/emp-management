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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 기상청 단기예보 API 프록시
 * GET /api/weather/today?nx=60&ny=127
 *   → 오늘 날씨 (기온·강수확률·하늘상태·강수형태) + 재택 추천 여부 반환
 */
@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    @Value("${weather.api.key:}")
    private String apiKey;

    /** 기상청 단기예보 base_time 후보 (정시 기준) */
    private static final int[] BASE_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @GetMapping("/today")
    public ResponseEntity<?> today(
            @RequestParam(defaultValue = "60")  int nx,
            @RequestParam(defaultValue = "127") int ny) {

        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.ok(Map.of("error", "API_KEY_NOT_SET"));
        }

        try {
            LocalDateTime now = LocalDateTime.now(KST);
            int currentHour  = now.getHour();
            String today     = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // ── base_date / base_time 계산 (발표 후 약 10분 뒤 반영 → 1시간 여유) ──
            int    baseHour = -1;
            String baseDate = today;
            for (int i = BASE_HOURS.length - 1; i >= 0; i--) {
                if (BASE_HOURS[i] <= currentHour - 1) { baseHour = BASE_HOURS[i]; break; }
            }
            if (baseHour == -1) {               // 새벽 00~02시 → 전날 23시 사용
                baseHour = 23;
                baseDate = now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            }
            String baseTime = String.format("%02d00", baseHour);

            // ── 기상청 API 호출 ──
            String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            String url = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst"
                    + "?serviceKey=" + encodedKey
                    + "&pageNo=1&numOfRows=300&dataType=JSON"
                    + "&base_date=" + baseDate
                    + "&base_time=" + baseTime
                    + "&nx=" + nx + "&ny=" + ny;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            // ── JSON 파싱 ──
            ObjectMapper mapper = new ObjectMapper();
            JsonNode items = mapper.readTree(resp.body())
                    .path("response").path("body").path("items").path("item");

            if (!items.isArray()) {
                // 기상청 resultCode/resultMsg 추출해서 그대로 반환 (디버그용)
                JsonNode root = mapper.readTree(resp.body());
                String resultCode = root.path("response").path("header").path("resultCode").asText("UNKNOWN");
                String resultMsg  = root.path("response").path("header").path("resultMsg").asText("응답 오류");
                return ResponseEntity.status(502).body(Map.of(
                        "error",      "기상청 API 오류",
                        "resultCode", resultCode,
                        "resultMsg",  resultMsg
                ));
            }

            // 현재 시각과 가장 가까운 예보 시간 탐색 (오늘)
            String targetTime = String.format("%02d00", currentHour);
            String bestTime   = null;
            for (JsonNode item : items) {
                if (!today.equals(item.path("fcstDate").asText())) continue;
                String ft = item.path("fcstTime").asText();
                if (bestTime == null
                        || (ft.compareTo(targetTime) <= 0 && ft.compareTo(bestTime) > 0)) {
                    bestTime = ft;
                }
            }
            if (bestTime == null) bestTime = targetTime;

            // 해당 예보 시간대 카테고리 추출
            Map<String, String> w = new HashMap<>();
            for (JsonNode item : items) {
                if (!today.equals(item.path("fcstDate").asText())) continue;
                if (!bestTime.equals(item.path("fcstTime").asText())) continue;
                w.put(item.path("category").asText(), item.path("fcstValue").asText());
            }

            // 오늘 일 최대 강수확률 (악천후 판단용)
            int maxPop = 0;
            for (JsonNode item : items) {
                if (!today.equals(item.path("fcstDate").asText())) continue;
                if (!"POP".equals(item.path("category").asText())) continue;
                int p = item.path("fcstValue").asInt(0);
                if (p > maxPop) maxPop = p;
            }

            // ── 값 변환 ──
            int    sky = parseInt(w.getOrDefault("SKY", "1"));
            int    pty = parseInt(w.getOrDefault("PTY", "0"));
            int    pop = parseInt(w.getOrDefault("POP", "0"));
            int    tmp; try { tmp = (int) Double.parseDouble(w.getOrDefault("TMP", "20")); } catch (Exception e) { tmp = 20; }
            int    reh = parseInt(w.getOrDefault("REH", "60"));
            double wsd; try { wsd = Double.parseDouble(w.getOrDefault("WSD", "0")); } catch (Exception e) { wsd = 0; }

            String skyText = switch (sky) {
                case 1 -> "맑음"; case 3 -> "구름많음"; case 4 -> "흐림"; default -> "알 수 없음";
            };
            String ptyText = switch (pty) {
                case 1 -> "비"; case 2 -> "비/눈"; case 3 -> "눈"; case 4 -> "소나기"; default -> "없음";
            };
            String icon = pty > 0
                    ? switch (pty) { case 1, 4 -> "🌧️"; case 2 -> "🌨️"; case 3 -> "❄️"; default -> "🌦️"; }
                    : switch (sky) { case 1 -> "☀️"; case 3 -> "🌥️"; case 4 -> "☁️"; default -> "🌤️"; };

            // ── 재택 추천 판단 ──
            boolean          wfh     = maxPop >= 70 || pty == 1 || pty == 2 || pty == 3 || wsd >= 14.0;
            List<String>     reasons = new ArrayList<>();
            if (maxPop >= 70)  reasons.add("강수확률 " + maxPop + "%");
            if (pty == 1)      reasons.add("비");
            if (pty == 2)      reasons.add("비/눈");
            if (pty == 3)      reasons.add("눈");
            if (wsd >= 14.0)   reasons.add("강풍 " + wsd + "m/s");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("sky",        sky);
            result.put("pty",        pty);
            result.put("pop",        pop);
            result.put("maxPop",     maxPop);
            result.put("tmp",        tmp);
            result.put("reh",        reh);
            result.put("wsd",        wsd);
            result.put("skyText",    skyText);
            result.put("ptyText",    ptyText);
            result.put("icon",       icon);
            result.put("wfh",        wfh);
            result.put("wfhReasons", reasons);
            result.put("baseTime",   baseDate + " " + baseTime);
            result.put("fcstTime",   bestTime);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "날씨 조회 실패: " + e.getMessage()));
        }
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
}
