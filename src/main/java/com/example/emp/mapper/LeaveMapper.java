package com.example.emp.mapper;

import com.example.emp.model.LeaveBalance;
import com.example.emp.model.LeaveRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LeaveMapper {

    /* ── 공통 ── */
    LeaveRequest findById(@Param("leaveId") Long leaveId);

    /* ── 사원: 본인 내역 ── */
    List<LeaveRequest> findByEmpno(@Param("empno") Integer empno);

    /* ── 사원: 연차 잔여 ── */
    LeaveBalance findBalance(@Param("empno") Integer empno, @Param("year") int year);
    void initBalance(@Param("empno") Integer empno, @Param("year") int year);

    /* ── 사원: 신청 ── */
    void insert(LeaveRequest req);

    /* ── 사원: 취소 (PENDING 상태만) ── */
    void deleteByIdAndEmpno(@Param("leaveId") Long leaveId, @Param("empno") Integer empno);

    /* ── MGR: 1차 승인 대기 목록 ── */
    List<LeaveRequest> findPendingByMgr(@Param("mgrEmpno") Integer mgrEmpno);

    /* ── MGR: 1차 승인/반려 ── */
    void updateMgrDecision(LeaveRequest req);

    /* ── ADMIN: 전체 목록 ── */
    List<LeaveRequest> findAll();

    /* ── ADMIN: 전체 사원 연차 잔여 현황 ── */
    List<LeaveBalance> findAllBalances(@Param("year") int year);

    /* ── ADMIN: 최종 승인/반려 ── */
    int updateAdminDecision(LeaveRequest req);

    /* ── ADMIN: 최종 승인 시 used_days 증가 ── */
    void increaseUsedDays(@Param("empno") Integer empno, @Param("year") int year, @Param("days") Double days);

    /* ── ADMIN: 최종 반려 시 used_days 복구 (이미 승인된 것 반려 시) ── */
    void decreaseUsedDays(@Param("empno") Integer empno, @Param("year") int year, @Param("days") Double days);
}
