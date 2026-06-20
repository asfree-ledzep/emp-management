package com.example.emp.service;

import com.example.emp.mapper.EmpMapper;
import com.example.emp.mapper.WorkLogMapper;
import com.example.emp.model.Emp;
import com.example.emp.model.WorkLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkLogService {

    @Autowired private WorkLogMapper workLogMapper;
    @Autowired private EmpMapper     empMapper;

    // ── 사원: 임시저장 / 신규 저장 ────────────────────────────
    public WorkLog save(Integer empno, WorkLog req) {
        Emp emp = empMapper.findById(empno);
        if (emp == null) throw new IllegalArgumentException("사원 정보를 찾을 수 없습니다.");

        long id = workLogMapper.nextVal();
        req.setLogId(id);
        req.setEmpno(empno);
        req.setMgrEmpno(emp.getMgr());
        req.setStatus("DRAFT");
        workLogMapper.insert(req);
        return workLogMapper.findById(id);
    }

    // ── 사원: 수정 (DRAFT / MGR_REJECTED / ADMIN_REJECTED) ───
    public WorkLog edit(Integer empno, Long logId, WorkLog req) {
        WorkLog existing = workLogMapper.findById(logId);
        if (existing == null || !existing.getEmpno().equals(empno))
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        if (!List.of("DRAFT","MGR_REJECTED","ADMIN_REJECTED").contains(existing.getStatus()))
            throw new IllegalArgumentException("현재 상태에서는 수정할 수 없습니다.");

        req.setLogId(logId);
        req.setEmpno(empno);
        req.setStatus(existing.getStatus()); // 상태 유지
        workLogMapper.update(req);
        return workLogMapper.findById(logId);
    }

    // ── 사원: 제출 (DRAFT → SUBMITTED) ───────────────────────
    public void submit(Integer empno, Long logId) {
        WorkLog log = workLogMapper.findById(logId);
        if (log == null || !log.getEmpno().equals(empno))
            throw new IllegalArgumentException("제출 권한이 없습니다.");
        if (!List.of("DRAFT","MGR_REJECTED","ADMIN_REJECTED").contains(log.getStatus()))
            throw new IllegalArgumentException("현재 상태에서는 제출할 수 없습니다.");
        if (log.getMgrEmpno() == null)
            throw new IllegalArgumentException("직속상관이 지정되지 않았습니다.");

        workLogMapper.updateStatus(logId, "SUBMITTED", null, null, null, null);
    }

    // ── 사원: 삭제 (DRAFT만) ─────────────────────────────────
    public void delete(Integer empno, Long logId) {
        WorkLog log = workLogMapper.findById(logId);
        if (log == null || !log.getEmpno().equals(empno))
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        if (!"DRAFT".equals(log.getStatus()))
            throw new IllegalArgumentException("임시저장 상태만 삭제할 수 있습니다.");
        workLogMapper.delete(logId);
    }

    // ── 사원: 본인 목록 ──────────────────────────────────────
    public List<WorkLog> getMyLogs(Integer empno) {
        return workLogMapper.findByEmpno(empno);
    }

    public WorkLog getById(Long logId) {
        return workLogMapper.findById(logId);
    }

    // ── 팀장: 결재 대기 목록 ─────────────────────────────────
    public List<WorkLog> getPendingForMgr(Integer mgrEmpno) {
        return workLogMapper.findPendingForMgr(mgrEmpno);
    }

    public List<WorkLog> getAllForMgr(Integer mgrEmpno) {
        return workLogMapper.findAllForMgr(mgrEmpno);
    }

    // ── 팀장: 승인 ───────────────────────────────────────────
    public void mgrApprove(Integer mgrEmpno, Long logId, String comment) {
        WorkLog log = workLogMapper.findById(logId);
        if (log == null || !mgrEmpno.equals(log.getMgrEmpno()))
            throw new IllegalArgumentException("결재 권한이 없습니다.");
        if (!"SUBMITTED".equals(log.getStatus()))
            throw new IllegalArgumentException("결재 대기 상태가 아닙니다.");

        workLogMapper.updateStatus(logId, "MGR_APPROVED", null, null, comment, null);
        workLogMapper.setMgrAt(logId);
    }

    // ── 팀장: 반려 ───────────────────────────────────────────
    public void mgrReject(Integer mgrEmpno, Long logId, String reason, String comment) {
        WorkLog log = workLogMapper.findById(logId);
        if (log == null || !mgrEmpno.equals(log.getMgrEmpno()))
            throw new IllegalArgumentException("결재 권한이 없습니다.");
        if (!"SUBMITTED".equals(log.getStatus()))
            throw new IllegalArgumentException("결재 대기 상태가 아닙니다.");

        workLogMapper.updateStatus(logId, "MGR_REJECTED", reason, "MGR", comment, null);
        workLogMapper.setMgrAt(logId);
    }

    // ── 관리자: 확인 대기 목록 ───────────────────────────────
    public List<WorkLog> getPendingForAdmin() {
        return workLogMapper.findPendingForAdmin();
    }

    public List<WorkLog> getAllLogs() {
        return workLogMapper.findAll();
    }

    // ── 관리자: 최종 확인 ────────────────────────────────────
    public void adminApprove(Long logId, String comment) {
        WorkLog log = workLogMapper.findById(logId);
        if (log == null) throw new IllegalArgumentException("업무일지를 찾을 수 없습니다.");
        if (!"MGR_APPROVED".equals(log.getStatus()))
            throw new IllegalArgumentException("팀장 승인 상태가 아닙니다.");

        workLogMapper.updateStatus(logId, "APPROVED", null, null, null, comment);
        workLogMapper.setAdminAt(logId);
    }

    // ── 관리자: 반려 ─────────────────────────────────────────
    public void adminReject(Long logId, String reason, String comment) {
        WorkLog log = workLogMapper.findById(logId);
        if (log == null) throw new IllegalArgumentException("업무일지를 찾을 수 없습니다.");
        if (!"MGR_APPROVED".equals(log.getStatus()))
            throw new IllegalArgumentException("팀장 승인 상태가 아닙니다.");

        workLogMapper.updateStatus(logId, "ADMIN_REJECTED", reason, "ADMIN", null, comment);
        workLogMapper.setAdminAt(logId);
    }
}
