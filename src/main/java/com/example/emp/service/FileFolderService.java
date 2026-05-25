package com.example.emp.service;

import com.example.emp.mapper.FileFolderMapper;
import com.example.emp.model.FileFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileFolderService {

    @Autowired private FileFolderMapper mapper;

    /** 루트 폴더 목록 */
    public List<FileFolder> getRootFolders(String scope, Integer deptno) {
        return mapper.findRootFolders(scope, deptno);
    }

    /** 하위 폴더 목록 */
    public List<FileFolder> getSubFolders(Long parentId, String scope, Integer deptno) {
        return mapper.findByParentId(parentId, scope, deptno);
    }

    /** 폴더 단건 */
    public FileFolder findById(Long folderId) {
        return mapper.findById(folderId);
    }

    /** 폴더 생성 */
    public FileFolder create(String folderName, Long parentId, String scope,
                             Integer deptno, String creator, String creatorName) {
        FileFolder ff = new FileFolder();
        ff.setFolderName(folderName.trim());
        ff.setParentId(parentId);
        ff.setScope(scope);
        ff.setDeptno(deptno);
        ff.setCreator(creator);
        ff.setCreatorName(creatorName);
        mapper.insert(ff);
        return ff;
    }

    /** 폴더 이름 변경 */
    public void rename(Long folderId, String newName, String requesterKey, boolean isAdmin) {
        FileFolder ff = mapper.findById(folderId);
        if (ff == null) throw new IllegalArgumentException("폴더가 없습니다.");
        if (!isAdmin && !ff.getCreator().equals(requesterKey)) {
            throw new IllegalArgumentException("본인이 만든 폴더만 수정할 수 있습니다.");
        }
        mapper.updateName(folderId, newName.trim());
    }

    /**
     * 폴더 삭제 (하위 파일은 루트로 이동, 하위 폴더는 재귀 삭제)
     * 단순 구현: 해당 폴더의 파일 연결 해제 후 폴더 삭제
     * (하위 폴더 재귀 삭제는 프론트에서 빈 폴더만 삭제 가능하도록 제한)
     */
    public void delete(Long folderId, String requesterKey, boolean isAdmin) {
        FileFolder ff = mapper.findById(folderId);
        if (ff == null) throw new IllegalArgumentException("폴더가 없습니다.");
        if (!isAdmin && !ff.getCreator().equals(requesterKey)) {
            throw new IllegalArgumentException("본인이 만든 폴더만 삭제할 수 있습니다.");
        }
        // 하위 파일 연결 해제 (파일 자체는 삭제하지 않고 루트로 이동)
        mapper.clearFolderFiles(folderId);
        mapper.deleteById(folderId);
    }
}
