package com.example.emp.controller;

import com.example.emp.model.Holiday;
import com.example.emp.service.HolidayService;
import com.example.emp.service.PublicHolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    @Autowired private HolidayService       holidayService;
    @Autowired private PublicHolidayService publicHolidayService;

    @Value("${public.holiday.api.key}")
    private String apiKey;

    // ─── 연도별 전체 (공공 API + CUSTOM 병합) ────────────────────────────
    @GetMapping
    public ResponseEntity<List<Holiday>> getByYear(@RequestParam int year) {
        return ResponseEntity.ok(holidayService.getMergedByYear(year));
    }

    // ─── 연월별 (공공 API + CUSTOM 병합) ─ 챗봇 / 사원 페이지 ──────────
    @GetMapping("/month")
    public ResponseEntity<List<Holiday>> getByMonth(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(holidayService.getMergedByMonth(year, month));
    }

    // ─── 관리자 직접 입력 등록 ────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody Holiday holiday,
            Authentication auth) {
        if (!isAdmin(auth))
            return ResponseEntity.status(403).body(Map.of("message", "관리자만 등록 가능합니다."));
        holidayService.create(holiday, auth.getName());
        return ResponseEntity.ok(Map.of("message", "공휴일 등록 완료"));
    }

    // ─── 관리자 삭제 (CUSTOM 항목만) ─────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            Authentication auth) {
        if (!isAdmin(auth))
            return ResponseEntity.status(403).body(Map.of("message", "관리자만 삭제 가능합니다."));
        holidayService.delete(id);
        return ResponseEntity.ok(Map.of("message", "삭제 완료"));
    }

    // ─── 디버그 (공공 API 원문 확인) ──────────────────────────────────────
    @GetMapping("/debug")
    public ResponseEntity<String> debug(
            @RequestParam(defaultValue = "2026") int year,
            @RequestParam(defaultValue = "6")   int month) {
        String url = "http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo"
                + "?ServiceKey=" + apiKey
                + "&solYear=" + year
                + "&solMonth=" + String.format("%02d", month)
                + "&_type=json&numOfRows=50";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            ResponseEntity<String> resp = new RestTemplate()
                    .exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return ResponseEntity.ok(resp.getBody());
        } catch (Exception e) {
            return ResponseEntity.ok("ERROR: " + e.getMessage());
        }
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
