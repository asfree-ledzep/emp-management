package com.example.emp.controller;

import com.example.emp.model.Faq;
import com.example.emp.service.FaqService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/faq")
public class FaqController {

    @Autowired
    private FaqService faqService;

    // 전체 조회 (관리자 전용 - 관리 화면)
    @GetMapping
    public ResponseEntity<List<Faq>> getAll() {
        return ResponseEntity.ok(faqService.getAll());
    }

    // 챗봇 검색 (모든 사용자)
    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody Map<String, String> body) {
        String query = body.getOrDefault("query", "");
        Faq result = faqService.search(query);
        if (result == null) {
            return ResponseEntity.ok(Map.of("found", false, "message", "죄송합니다. 관련 답변을 찾지 못했습니다. 관리자에게 문의해 주세요."));
        }
        return ResponseEntity.ok(Map.of(
                "found",    true,
                "faqId",    result.getFaqId(),
                "category", result.getCategory() != null ? result.getCategory() : "",
                "question", result.getQuestion(),
                "answer",   result.getAnswer()
        ));
    }

    // 등록 (관리자만)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Faq faq, Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(Map.of("error", "관리자만 등록 가능합니다."));
        faq.setCreatedBy(auth.getName());
        faqService.create(faq);
        return ResponseEntity.ok(Map.of("message", "FAQ 등록 완료"));
    }

    // 수정 (관리자만)
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Faq faq, Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(Map.of("error", "관리자만 수정 가능합니다."));
        faqService.update(id, faq);
        return ResponseEntity.ok(Map.of("message", "FAQ 수정 완료"));
    }

    // 삭제 (관리자만)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(Map.of("error", "관리자만 삭제 가능합니다."));
        faqService.delete(id);
        return ResponseEntity.ok(Map.of("message", "FAQ 삭제 완료"));
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
