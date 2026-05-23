package com.example.emp.service;

import com.example.emp.mapper.FaqMapper;
import com.example.emp.mapper.HolidayMapper;
import com.example.emp.model.Faq;
import com.example.emp.model.Holiday;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class FaqService {

    @Autowired
    private FaqMapper faqMapper;

    @Autowired
    private HolidayMapper holidayMapper;

    // 휴무일 관련 키워드
    private static final Set<String> HOLIDAY_KEYWORDS =
        Set.of("휴무일", "휴무", "공휴일", "쉬는날", "쉬는 날", "휴일", "빨간날", "법정공휴일");

    public List<Faq> getAll() {
        return faqMapper.findAll();
    }

    public Faq getById(Long id) {
        return faqMapper.findById(id);
    }

    public void create(Faq faq) {
        faqMapper.insert(faq);
    }

    public void update(Long id, Faq faq) {
        faq.setFaqId(id);
        faqMapper.update(faq);
    }

    public void delete(Long id) {
        faqMapper.delete(id);
    }

    /**
     * 키워드 매칭으로 가장 연관성 높은 FAQ 반환.
     * 휴무일 키워드 감지 시 HOLIDAY 테이블에서 동적 조회하여 반환.
     */
    public Faq search(String query) {
        if (query == null || query.isBlank()) return null;
        String q = query.toLowerCase().trim();

        // ── 휴무일 동적 조회 ──
        if (isHolidayQuery(q)) {
            return buildHolidayFaq(q);
        }

        // ── 일반 FAQ 키워드 매칭 ──
        List<Faq> all = faqMapper.findAll();
        Faq best = null;
        int bestScore = 0;

        for (Faq faq : all) {
            int score = 0;
            if (faq.getQuestion() != null && faq.getQuestion().toLowerCase().contains(q)) score += 10;
            if (faq.getKeywords() != null) {
                for (String kw : faq.getKeywords().split(",")) {
                    String k = kw.trim().toLowerCase();
                    if (k.isEmpty()) continue;
                    if (q.contains(k)) score += 5;
                    else if (k.contains(q)) score += 2;
                }
            }
            if (faq.getAnswer() != null && faq.getAnswer().toLowerCase().contains(q)) score += 1;
            if (score > bestScore) { bestScore = score; best = faq; }
        }
        return bestScore >= 2 ? best : null;
    }

    // 휴무일 키워드 포함 여부
    private boolean isHolidayQuery(String q) {
        return HOLIDAY_KEYWORDS.stream().anyMatch(q::contains);
    }

    // 질문에서 월 추출 → HOLIDAY 테이블 조회 → Faq 객체 생성
    private Faq buildHolidayFaq(String q) {
        LocalDate today = LocalDate.now();
        int year  = today.getYear();
        int month = parseMonth(q, today);

        List<Holiday> holidays = holidayMapper.findByYearMonth(year, month);

        String monthLabel = month + "월";
        String answer;
        if (holidays.isEmpty()) {
            answer = monthLabel + "에는 등록된 공휴일이 없습니다.";
        } else {
            StringBuilder sb = new StringBuilder(monthLabel + " 공휴일 " + holidays.size() + "건\n\n");
            for (Holiday h : holidays) {
                // holidayDate: "YYYY-MM-DD" → "M/D"
                String[] parts = h.getHolidayDate().split("-");
                String mmdd = Integer.parseInt(parts[1]) + "/" + Integer.parseInt(parts[2]);
                sb.append("📅 ").append(mmdd).append("  ").append(h.getHolidayName()).append("\n");
            }
            answer = sb.toString().trim();
        }

        Faq faq = new Faq();
        faq.setFaqId(-1L);
        faq.setCategory("휴무일");
        faq.setQuestion(monthLabel + " 공휴일");
        faq.setAnswer(answer);
        return faq;
    }

    // 질문 텍스트에서 월 파싱
    // "3월" → 3,  "다음 달" → 현재+1,  "저번 달/지난 달" → 현재-1,  없으면 → 현재 월
    private int parseMonth(String q, LocalDate today) {
        // 숫자 + "월" 패턴
        java.util.regex.Matcher m =
            java.util.regex.Pattern.compile("(\\d{1,2})월").matcher(q);
        if (m.find()) {
            int mo = Integer.parseInt(m.group(1));
            if (mo >= 1 && mo <= 12) return mo;
        }
        // 상대 표현
        if (q.contains("다음달") || q.contains("다음 달") || q.contains("next")) {
            return today.plusMonths(1).getMonthValue();
        }
        if (q.contains("저번달") || q.contains("저번 달") || q.contains("지난달") || q.contains("지난 달")) {
            return today.minusMonths(1).getMonthValue();
        }
        return today.getMonthValue(); // 기본값: 이번 달
    }
}
