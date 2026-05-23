package com.example.emp.service;

import com.example.emp.mapper.FaqMapper;
import com.example.emp.model.Faq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FaqService {

    @Autowired
    private FaqMapper faqMapper;

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

    // 키워드 매칭으로 가장 연관성 높은 FAQ 반환
    // 공휴일 질문은 ChatbotModal에서 PublicHolidayService로 직접 처리
    public Faq search(String query) {
        if (query == null || query.isBlank()) return null;
        String q = query.toLowerCase().trim();

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
}
