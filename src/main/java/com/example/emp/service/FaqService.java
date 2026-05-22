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

    /**
     * 키워드 매칭으로 가장 연관성 높은 FAQ 반환
     * 점수 계산:
     *   - 질문 전체 포함   : +10점
     *   - 키워드 완전 포함  : +5점
     *   - 키워드 부분 포함  : +2점
     */
    public Faq search(String query) {
        if (query == null || query.isBlank()) return null;

        List<Faq> all = faqMapper.findAll();
        String q = query.toLowerCase().trim();

        Faq best = null;
        int bestScore = 0;

        for (Faq faq : all) {
            int score = 0;

            // 질문 자체에 포함
            if (faq.getQuestion() != null && faq.getQuestion().toLowerCase().contains(q)) {
                score += 10;
            }

            // 키워드 매칭
            if (faq.getKeywords() != null) {
                String[] kws = faq.getKeywords().split(",");
                for (String kw : kws) {
                    String k = kw.trim().toLowerCase();
                    if (k.isEmpty()) continue;
                    if (q.contains(k)) score += 5;       // 입력에 키워드 포함
                    else if (k.contains(q)) score += 2;  // 키워드에 입력 포함
                }
            }

            // 답변 자체에도 포함되면 가산
            if (faq.getAnswer() != null && faq.getAnswer().toLowerCase().contains(q)) {
                score += 1;
            }

            if (score > bestScore) {
                bestScore = score;
                best = faq;
            }
        }

        return bestScore >= 2 ? best : null; // 최소 점수 이상일 때만 반환
    }
}
