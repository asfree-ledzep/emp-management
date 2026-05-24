package com.example.emp.mapper;

import com.example.emp.model.FileShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileShareMapper {

    void insert(FileShare file);

    FileShare findById(Long fileId);

    /** scope = ALL : 전체 공유 파일 */
    List<FileShare> findByScope(@Param("scope") String scope);

    /** 특정 부서 파일 (scope=DEPT, deptno=N) */
    List<FileShare> findByDeptno(@Param("deptno") Integer deptno);

    /** 관리자용 동적 필터 (scope, deptno 모두 nullable) */
    List<FileShare> findAllAdmin(@Param("scope") String scope,
                                 @Param("deptno") Integer deptno);

    void deleteById(@Param("fileId") Long fileId);
}
