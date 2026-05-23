package com.example.emp.controller;

import com.example.emp.model.Holiday;
import com.example.emp.service.PublicHolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    @Autowired
    private PublicHolidayService publicHolidayService;

    @Value("${public.holiday.api.key}")
    private String apiKey;

    // 연월별 공휴일 조회
    @GetMapping("/month")
    public ResponseEntity<List<Holiday>> getByMonth(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(publicHolidayService.getHolidays(year, month));
    }

    // 공공 API 원문 응답 확인용 (관리자 디버그)
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
}
