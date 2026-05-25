package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FileFolder {
    private Long    folderId;
    private String  folderName;
    private Long    parentId;       // null = 루트
    private String  scope;          // ALL | DEPT
    private Integer deptno;
    private String  creator;        // empno 문자열 or 'admin'
    private String  creatorName;    // 표시용 이름
    private String  createdAt;      // TO_CHAR 포맷
    private Integer fileCount;      // 하위 파일 수 (JOIN)
    private Integer subFolderCount; // 하위 폴더 수 (JOIN)
}
