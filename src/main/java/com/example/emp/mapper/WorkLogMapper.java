package com.example.emp.mapper;

import com.example.emp.model.WorkLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WorkLogMapper {
    void insert(WorkLog log);
    void update(WorkLog log);
    void updateStatus(@Param("logId") Long logId,
                      @Param("status") String status,
                      @Param("rejectReason") String rejectReason,
                      @Param("rejectedBy") String rejectedBy,
                      @Param("mgrComment") String mgrComment,
                      @Param("adminComment") String adminComment);
    void setMgrAt(@Param("logId") Long logId);
    void setAdminAt(@Param("logId") Long logId);

    WorkLog findById(Long logId);

    /** 사원 본인 목록 */
    List<WorkLog> findByEmpno(Integer empno);

    /** 팀장: 결재 대기 (SUBMITTED) */
    List<WorkLog> findPendingForMgr(Integer mgrEmpno);

    /** 팀장: 본인 팀원 전체 */
    List<WorkLog> findAllForMgr(Integer mgrEmpno);

    /** 관리자: 2차 확인 대기 (MGR_APPROVED) */
    List<WorkLog> findPendingForAdmin();

    /** 관리자: 전체 목록 */
    List<WorkLog> findAll();

    void delete(Long logId);

    long nextVal();
}
