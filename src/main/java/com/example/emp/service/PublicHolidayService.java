package com.example.emp.service;

import com.example.emp.model.Holiday;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class PublicHolidayService {

    @Value("${public.holiday.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BASE_URL =
            "http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo";

    public List<Holiday> getHolidays(int year, int month) {
        String url = BASE_URL
                + "?ServiceKey=" + apiKey
                + "&solYear=" + year
                + "&solMonth=" + String.format("%02d", month)
                + "&_type=json"
                + "&numOfRows=50";

        try {
            // Accept: application/json 헤더 명시
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            String body = response.getBody();
            if (body == null) return List.of();

            JsonNode root = objectMapper.readTree(body);
            JsonNode item = root.path("response").path("body").path("items").path("item");

            List<Holiday> result = new ArrayList<>();
            if (item.isArray()) {
                item.forEach(n -> result.add(toHoliday(n)));
            } else if (!item.isMissingNode() && !item.isNull() && item.isObject()) {
                result.add(toHoliday(item));
            }
            return result;

        } catch (Exception e) {
            return List.of();
        }
    }

    private Holiday toHoliday(JsonNode n) {
        String raw = String.valueOf(n.path("locdate").asLong()); // 예: 20260606
        String date = raw.substring(0, 4) + "-" + raw.substring(4, 6) + "-" + raw.substring(6, 8);
        Holiday h = new Holiday();
        h.setHolidayDate(date);
        h.setHolidayEndDate(date);
        h.setHolidayName(n.path("dateName").asText());
        return h;
    }
}
