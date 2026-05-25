package com.example.emp.mapper;

import com.example.emp.model.FileShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileShareMapper {

    void insert(FileShare file);

    FileShare findById(Long fileId);

    /** scope = ALL : 전체 공유 파일 (folderId 필터 포함) */
    List<FileShare> findByScope(@Param("scope")    String  scope,
                                @Param("folderId") Long    folderId);

    /** 특정 부서 파일 (scope=DEPT, deptno=N, folderId 필터 포함) */
    List<FileShare> findByDeptno(@Param("deptno")   Integer deptno,
                                 @Param("folderId") Long    folderId);

    /** 관리자용 동적 필터 (scope, deptno, folderId 모두 nullable) */
    List<FileShare> findAllAdmin(@Param("scope")    String  scope,
                                 @Param("deptno")   Integer deptno,
                                 @Param("folderId") Long    folderId);

    /** 파일 폴더 이동 */
    void moveToFolder(@Param("fileId")   Long fileId,
                      @Param("folderId") Long folderId);

    void deleteById(@Param("fileId") Long fileId);
}
