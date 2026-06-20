package com.example.emp.service;

import com.example.emp.mapper.EmpMapper;
import com.example.emp.mapper.LeaveMapper;
import com.example.emp.model.Emp;
import com.example.emp.model.LeaveBalance;
import com.example.emp.model.LeaveRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class LeaveService {

    @Autowired private LeaveMapper leaveMapper;
    @Autowired private EmpMapper   empMapper;

    // ── 사원: 잔여 연차 조회 (없으면 초기화) ──
    public LeaveBalance getBalance(Integer empno) {
        int year = LocalDate.now().getYear();
        LeaveBalance bal = leaveMapper.findBalance(empno, year);
        if (bal == null) {
            leaveMapper.initBalance(empno, year);
            bal = leaveMapper.findBalance(empno, year);
        }
        return bal;
    }

    // ── 사원: 본인 내역 ──
    public List<LeaveRequest> getMyLeaves(Integer empno) {
        return leaveMapper.findByEmpno(empno);
    }

    // ── 사원: 연차 신청 ──
    @Transactional
    public void apply(Integer empno, LeaveRequest req) {
        // days 미전송 시 startDate~endDate로 자동 계산
        if (req.getDays() == null) {
            long d = ChronoUnit.DAYS.between(req.getStartDate(), req.getEndDate()) + 1;
            req.setDays((double) d);
        }
        // 잔여 연차 확인
        LeaveBalance bal = getBalance(empno);
        if (req.getDays() > bal.getRemaining()) {
            throw new IllegalArgumentException("잔여 연차가 부족합니다. (잔여: " + bal.getRemaining() + "일)");
        }
        // 직속 상관 자동 설정
        Emp emp = empMapper.findById(empno);
        if (emp == null) throw new IllegalArgumentException("사원 정보가 없습니다.");
        req.setEmpno(empno);
        req.setMgrEmpno(emp.getMgr());  // EMP.MGR 컬럼
        leaveMapper.insert(req);
    }

    // ── 사원: 신청 취소 (PENDING만) ──
    public void cancel(Long leaveId, Integer empno) {
        leaveMapper.deleteByIdAndEmpno(leaveId, empno);
    }

    // ── MGR: 1차 승인 대기 목록 ──
    public List<LeaveRequest> getPendingForMgr(Integer mgrEmpno) {
        return leaveMapper.findPendingByMgr(mgrEmpno);
    }

    // ── MGR: 1차 승인/반려 ──
    @Transactional
    public void mgrDecide(Long leaveId, Integer mgrEmpno, boolean approve, String comment) {
        LeaveRequest req = leaveMapper.findById(leaveId);
        if (req == null) throw new IllegalArgumentException("신청 내역이 없습니다.");
        req.setLeaveId(leaveId);
        req.setMgrEmpno(mgrEmpno);
        req.setStatus(approve ? "MGR_APPROVED" : "MGR_REJECTED");
        req.setMgrComment(comment);
        leaveMapper.updateMgrDecision(req);
    }

    // ── ADMIN: 전체 목록 ──
    public List<LeaveRequest> getAll() {
        return leaveMapper.findAll();
    }

    // ── ADMIN: 최종 승인 ──
    @Transactional
    public void adminApprove(Long leaveId, String comment) {
        LeaveRequest req = leaveMapper.findById(leaveId);
        if (req == null) throw new IllegalArgumentException("신청 내역이 없습니다.");
        req.setStatus("APPROVED");
        req.setAdminComment(comment);
        leaveMapper.updateAdminDecision(req);
        // 사용 연차 차감
        int year = req.getStartDate().getYear();
        // LEAVE_BALANCE 행 없으면 초기화
        LeaveBalance bal = leaveMapper.findBalance(req.getEmpno(), year);
        if (bal == null) leaveMapper.initBalance(req.getEmpno(), year);
        leaveMapper.increaseUsedDays(req.getEmpno(), year, req.getDays());
    }

    // ── ADMIN: 최종 반려 ──
    @Transactional
    public void adminReject(Long leaveId, String comment) {
        LeaveRequest req = leaveMapper.findById(leaveId);
        if (req == null) throw new IllegalArgumentException("신청 내역이 없습니다.");
        req.setStatus("REJECTED");
        req.setAdminComment(comment);
        leaveMapper.updateAdminDecision(req);
        // 이미 APPROVED가 되어 차감된 적 없으므로 복구 불필요
        // (MGR_APPROVED → REJECTED)
    }

    // ── ADMIN: 연차 잔여 조회 ──
    public LeaveBalance getBalanceForAdmin(Integer empno) {
        return getBalance(empno);
    }

    // ── ADMIN: 전체 사원 연차 잔여 현황 ──
    public List<LeaveBalance> getAllBalances(int year) {
        return leaveMapper.findAllBalances(year);
    }
}
