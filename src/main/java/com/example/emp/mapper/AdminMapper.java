package com.example.emp.mapper;

import com.example.emp.model.AdminProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminMapper {

    /** 관리자 프로필 조회 (없으면 null) */
    AdminProfile findProfile();

    /** 관리자 프로필 저장 (MERGE) */
    void upsertProfile(AdminProfile profile);

    /** 대리 관리자 목록 (empno + ename + createdAt) */
    List<Map<String, Object>> findProxyAdmins();

    /** 대리 관리자 추가 (이미 있으면 무시) */
    void addProxyAdmin(@Param("empno") Integer empno);

    /** 대리 관리자 제거 */
    void removeProxyAdmin(@Param("empno") Integer empno);

    /** 특정 사번이 대리 관리자인지 확인 */
    boolean isProxyAdmin(@Param("empno") Integer empno);
}
