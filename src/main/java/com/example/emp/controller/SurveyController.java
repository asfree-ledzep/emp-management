package com.example.emp.controller;

import com.example.emp.model.*;
import com.example.emp.service.SurveyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/surveys")
public class SurveyController {

    private static final Logger log = LoggerFactory.getLogger(SurveyController.class);

    @Autowired
    private SurveyService surveyService;

    // 설문 목록 (전체 사용자)
    @GetMapping
    public ResponseEntity<?> findAll(Authentication auth) {
        List<Survey> surveys = surveyService.findAll();

        // 사원인 경우 응답 여부 포함
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            try {
                Integer empno = Integer.parseInt(auth.getName());
                surveys.forEach(s -> s.setResponseCount(
                        surveyService.hasResponded(s.getSurveyId(), empno) ? -1 : s.getResponseCount()
                ));
            } catch (NumberFormatException ignored) {}
        }
        return ResponseEntity.ok(surveys);
    }

    // 설문 상세 (문항+선택지)
    @GetMapping("/{surveyId}")
    public ResponseEntity<?> findById(@PathVariable Long surveyId) {
        Survey survey = surveyService.findById(surveyId);
        if (survey == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(survey);
    }

    // 설문 등록 (관리자만)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Survey survey, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) return ResponseEntity.status(403).build();

        try {
            Survey created = surveyService.create(survey, auth.getName());
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // 설문 마감 (관리자만)
    @PatchMapping("/{surveyId}/close")
    public ResponseEntity<?> close(@PathVariable Long surveyId, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) return ResponseEntity.status(403).build();

        surveyService.close(surveyId);
        return ResponseEntity.ok(Map.of("message", "설문이 마감되었습니다."));
    }

    // 설문 삭제 (관리자만)
    @DeleteMapping("/{surveyId}")
    public ResponseEntity<?> delete(@PathVariable Long surveyId, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) return ResponseEntity.status(403).build();

        surveyService.delete(surveyId);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    // 응답 제출 (사원)
    @PostMapping("/{surveyId}/responses")
    public ResponseEntity<?> submitResponse(
            @PathVariable Long surveyId,
            @RequestBody List<SurveyAnswer> answers,
            Authentication auth) {
        try {
            Integer empno = Integer.parseInt(auth.getName());
            surveyService.submitResponse(surveyId, empno, answers);
            return ResponseEntity.ok(Map.of("message", "응답이 저장되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 응답 여부 확인 (사원)
    @GetMapping("/{surveyId}/responded")
    public ResponseEntity<?> hasResponded(@PathVariable Long surveyId, Authentication auth) {
        try {
            Integer empno = Integer.parseInt(auth.getName());
            boolean responded = surveyService.hasResponded(surveyId, empno);
            return ResponseEntity.ok(Map.of("responded", responded));
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(Map.of("responded", false));
        }
    }

    // 설문 결과 (관리자만)
    @GetMapping("/{surveyId}/results")
    public ResponseEntity<?> getResults(@PathVariable Long surveyId, Authentication auth) {
        if (auth == null) {
            log.warn("[results] auth is null for surveyId={}", surveyId);
            return ResponseEntity.status(403).body("auth null");
        }
        String authorities = auth.getAuthorities().stream()
                .map(a -> a.getAuthority()).collect(java.util.stream.Collectors.joining(","));
        log.info("[results] surveyId={} name={} authorities={}", surveyId, auth.getName(), authorities);

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            log.warn("[results] FORBIDDEN surveyId={} name={} authorities={}", surveyId, auth.getName(), authorities);
            return ResponseEntity.status(403).body("not admin: " + authorities);
        }

        List<SurveyResultDto> results = surveyService.getResults(surveyId);
        return ResponseEntity.ok(results);
    }

    // 주관식 응답 목록 (관리자만)
    @GetMapping("/{surveyId}/questions/{questionId}/text-answers")
    public ResponseEntity<?> getTextAnswers(
            @PathVariable Long surveyId,
            @PathVariable Long questionId,
            Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) return ResponseEntity.status(403).build();

        List<String> answers = surveyService.getTextAnswers(surveyId, questionId);
        return ResponseEntity.ok(answers);
    }
}
