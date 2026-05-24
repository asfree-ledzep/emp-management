package com.example.emp.service;

import com.example.emp.mapper.HolidayMapper;
import com.example.emp.model.Holiday;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class HolidayService {

    @Autowired private HolidayMapper        holidayMapper;
    @Autowired private PublicHolidayService publicHolidayService;

    /**
     * 연도별 전체 조회 (관리 페이지용)
     * 공공 API 12개월 병렬 호출 + DB CUSTOM 항목 병합
     */
    public List<Holiday> getMergedByYear(int year) {
        // 공공 API: 1~12월 병렬 요청
        List<CompletableFuture<List<Holiday>>> futures = IntStream.rangeClosed(1, 12)
                .mapToObj(month -> CompletableFuture.supplyAsync(() -> {
                    List<Holiday> items = publicHolidayService.getHolidays(year, month);
                    items.forEach(h -> h.setSource("PUBLIC"));
                    return items;
                }))
                .collect(Collectors.toList());

        List<Holiday> publicList = futures.stream()
                .flatMap(f -> {
                    try { return f.get().stream(); }
                    catch (Exception e) { return java.util.stream.Stream.empty(); }
                })
                .collect(Collectors.toList());

        // DB CUSTOM 항목
        List<Holiday> customList = holidayMapper.findByYear(year);
        customList.forEach(h -> h.setSource("CUSTOM"));

        // 병합 + 날짜 정렬
        List<Holiday> merged = new ArrayList<>();
        merged.addAll(publicList);
        merged.addAll(customList);
        merged.sort(Comparator.comparing(Holiday::getHolidayDate));
        return merged;
    }

    /**
     * 연월별 조회 (챗봇 / 사원 페이지용)
     * 공공 API + DB CUSTOM 병합
     */
    public List<Holiday> getMergedByMonth(int year, int month) {
        List<Holiday> publicList = publicHolidayService.getHolidays(year, month);
        publicList.forEach(h -> h.setSource("PUBLIC"));

        List<Holiday> customList = holidayMapper.findByYearMonth(year, month);
        customList.forEach(h -> h.setSource("CUSTOM"));

        List<Holiday> merged = new ArrayList<>();
        merged.addAll(publicList);
        merged.addAll(customList);
        merged.sort(Comparator.comparing(Holiday::getHolidayDate));
        return merged;
    }

    /** 관리자 직접 입력 등록 */
    public void create(Holiday holiday, String createdBy) {
        holiday.setCreatedBy(createdBy);
        // holidayEndDate 미입력 시 holidayDate 와 동일하게
        if (holiday.getHolidayEndDate() == null || holiday.getHolidayEndDate().isBlank()) {
            holiday.setHolidayEndDate(holiday.getHolidayDate());
        }
        holidayMapper.insert(holiday);
    }

    /** 삭제 (CUSTOM 항목만 가능 – DB에 저장된 것만 holidayId 존재) */
    public void delete(Long id) {
        holidayMapper.delete(id);
    }
}
