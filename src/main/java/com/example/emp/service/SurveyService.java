package com.example.emp.service;

import com.example.emp.mapper.SurveyMapper;
import com.example.emp.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class SurveyService {

    @Autowired
    private SurveyMapper surveyMapper;

    @Autowired
    private KakaoService kakaoService;

    // 설문 목록
    public List<Survey> findAll() {
        return surveyMapper.findAll();
    }

    // 설문 상세 (문항+선택지 포함)
    public Survey findById(Long surveyId) {
        return surveyMapper.findById(surveyId);
    }

    // 설문 등록 (문항+선택지 일괄 저장)
    @Transactional
    public Survey create(Survey survey, String createdBy) {
        survey.setCreatedBy(createdBy);
        surveyMapper.insertSurvey(survey);

        if (survey.getQuestions() != null) {
            for (int i = 0; i < survey.getQuestions().size(); i++) {
                SurveyQuestion q = survey.getQuestions().get(i);
                q.setSurveyId(survey.getSurveyId());
                q.setOrderNum(i + 1);
                surveyMapper.insertQuestion(q);

                if (q.getOptions() != null) {
                    for (int j = 0; j < q.getOptions().size(); j++) {
                        SurveyOption o = q.getOptions().get(j);
                        o.setQuestionId(q.getQuestionId());
                        o.setOrderNum(j + 1);
                        surveyMapper.insertOption(o);
                    }
                }
            }
        }
        // 카카오톡 알림 발송
        String content = (survey.getDescription() != null && !survey.getDescription().isBlank())
                ? survey.getDescription() : "지금 바로 참여해보세요!";
        kakaoService.sendMessageToAll(
            "📋 [설문조사] " + survey.getTitle(),
            content,
            "https://emp-management-react.vercel.app/?page=survey"
        );

        return survey;
    }

    // 설문 마감
    public void close(Long surveyId) {
        surveyMapper.updateStatus(surveyId, "CLOSED");
    }

    // 설문 삭제
    public void delete(Long surveyId) {
        surveyMapper.deleteSurvey(surveyId);
    }

    // 응답 제출
    @Transactional
    public void submitResponse(Long surveyId, Integer empno, List<SurveyAnswer> answers) {
        // 중복 응답 방지
        if (surveyMapper.countResponse(surveyId, empno) > 0) {
            throw new RuntimeException("이미 응답한 설문입니다.");
        }

        SurveyResponse response = new SurveyResponse();
        response.setSurveyId(surveyId);
        response.setEmpno(empno);
        surveyMapper.insertResponse(response);

        for (SurveyAnswer answer : answers) {
            answer.setResponseId(response.getResponseId());
            surveyMapper.insertAnswer(answer);
        }
    }

    // 이미 응답했는지 확인
    public boolean hasResponded(Long surveyId, Integer empno) {
        return surveyMapper.countResponse(surveyId, empno) > 0;
    }

    // 설문 결과
    public List<SurveyResultDto> getResults(Long surveyId) {
        return surveyMapper.findResults(surveyId);
    }

    // 주관식 응답 목록
    public List<String> getTextAnswers(Long surveyId, Long questionId) {
        return surveyMapper.findTextAnswers(surveyId, questionId);
    }
}
