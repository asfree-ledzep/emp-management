package com.example.emp.mapper;

import com.example.emp.model.Attendance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AttendanceMapper {
    // 오늘 출근 기록 조회 (중복 방지)
    Attendance findTodayByEmpno(@Param("empno") Integer empno);

    // 출근 기록 삽입
    void insertCheckIn(Attendance attendance);

    // 퇴근 업데이트
    void updateCheckOut(@Param("empno") Integer empno);

    // 직원 자신의 월별 기록
    List<Attendance> findByEmpnoAndMonth(@Param("empno") Integer empno,
                                         @Param("month") String month);

    // 관리자: 전체 조회 (날짜, 부서 필터)
    List<Attendance> findAll(@Param("date") String date,
                             @Param("deptno") Integer deptno);

    // 오늘 전체 출근 현황 (QR 화면용)
    List<Attendance> findTodayAll();

    // 오늘 아직 출근하지 않은 사원 목록
    List<Attendance> findAbsentToday();
}
