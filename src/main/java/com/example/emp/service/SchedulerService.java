package com.example.emp.service;

import com.example.emp.mapper.SurveyMapper;
import com.example.emp.model.Survey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 스케줄러: 설문 마감 D-1 카카오톡 알림
 * 매일 오전 9시 실행
 */
@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    @Autowired
    private SurveyMapper surveyMapper;

    @Autowired
    private KakaoService kakaoService;

    /**
     * 매일 오전 9시: 내일 마감되는 ACTIVE 설문에 대해 카카오톡 알림 발송
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendSurveyExpiryAlert() {
        log.info("[스케줄러] 설문 마감 D-1 알림 시작");
        try {
            List<Survey> expiringSurveys = surveyMapper.findExpiringTomorrow();
            if (expiringSurveys.isEmpty()) {
                log.info("[스케줄러] 내일 마감 설문 없음");
                return;
            }
            for (Survey survey : expiringSurveys) {
                log.info("[스케줄러] D-1 알림 발송: surveyId={}, title={}", survey.getSurveyId(), survey.getTitle());
                kakaoService.sendMessageToAll(
                    "⏰ [설문 마감 임박] " + survey.getTitle(),
                    "내일(" + survey.getEndDate() + ")까지 응답해주세요!\n아직 참여하지 않으셨다면 지금 바로 참여해보세요.",
                    "https://emp-management-react.vercel.app/?page=survey"
                );
            }
            log.info("[스케줄러] 설문 마감 D-1 알림 완료: {}건", expiringSurveys.size());
        } catch (Exception e) {
            log.error("[스케줄러] 설문 마감 알림 오류: {}", e.getMessage(), e);
        }
    }
}
