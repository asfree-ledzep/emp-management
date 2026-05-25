package com.example.emp.mapper;

import com.example.emp.model.FileFolder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileFolderMapper {

    void insert(FileFolder folder);

    FileFolder findById(@Param("folderId") Long folderId);

    /** 루트 폴더 목록 (parentId IS NULL) - scope/deptno 필터 */
    List<FileFolder> findRootFolders(@Param("scope")  String  scope,
                                     @Param("deptno") Integer deptno);

    /** 특정 폴더의 하위 폴더 목록 */
    List<FileFolder> findByParentId(@Param("parentId") Long    parentId,
                                    @Param("scope")    String  scope,
                                    @Param("deptno")   Integer deptno);

    /** 폴더 이름 변경 */
    void updateName(@Param("folderId")   Long   folderId,
                    @Param("folderName") String folderName);

    void deleteById(@Param("folderId") Long folderId);

    /** 폴더 내 모든 파일 folderId 를 NULL 로 초기화 (폴더 삭제 전 처리) */
    void clearFolderFiles(@Param("folderId") Long folderId);
}
