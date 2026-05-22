package com.example.emp.mapper;

import com.example.emp.model.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SurveyMapper {

    // 설문 목록 (문항 미포함)
    List<Survey> findAll();

    // 설문 상세 (문항+선택지 포함)
    Survey findById(Long surveyId);

    // 설문 등록
    int insertSurvey(Survey survey);

    // 설문 상태 변경 (ACTIVE → CLOSED)
    int updateStatus(@Param("surveyId") Long surveyId, @Param("status") String status);

    // 설문 삭제
    int deleteSurvey(Long surveyId);

    // 문항 등록
    int insertQuestion(SurveyQuestion question);

    // 선택지 등록
    int insertOption(SurveyOption option);

    // 응답 헤더 등록
    int insertResponse(SurveyResponse response);

    // 응답 상세 등록
    int insertAnswer(SurveyAnswer answer);

    // 이미 응답했는지 확인
    int countResponse(@Param("surveyId") Long surveyId, @Param("empno") Integer empno);

    // 설문 결과 조회 (선택지별 응답 수)
    List<SurveyResultDto> findResults(Long surveyId);

    // 주관식 응답 목록
    List<String> findTextAnswers(@Param("surveyId") Long surveyId, @Param("questionId") Long questionId);

    // 내일 마감되는 ACTIVE 설문 목록 (D-1 알림용)
    List<Survey> findExpiringTomorrow();
}
