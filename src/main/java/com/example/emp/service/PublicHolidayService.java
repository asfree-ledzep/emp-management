package com.example.emp.service;

import com.example.emp.model.Holiday;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class PublicHolidayService {

    @Value("${public.holiday.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

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
            JsonNode root = restTemplate.getForObject(url, JsonNode.class);
            JsonNode item = root.path("response").path("body").path("items").path("item");

            List<Holiday> result = new ArrayList<>();
            if (item.isArray()) {
                item.forEach(n -> addIfHoliday(n, result));
            } else if (!item.isMissingNode() && !item.isNull()) {
                addIfHoliday(item, result);
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    private void addIfHoliday(JsonNode n, List<Holiday> list) {
        if (!"Y".equals(n.path("isHoliday").asText())) return;
        String raw = String.valueOf(n.path("locdate").asLong()); // 예: 20260603
        String date = raw.substring(0, 4) + "-" + raw.substring(4, 6) + "-" + raw.substring(6, 8);
        Holiday h = new Holiday();
        h.setHolidayDate(date);
        h.setHolidayEndDate(date);
        h.setHolidayName(n.path("dateName").asText());
        list.add(h);
    }
}
