package com.example.emp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FileShare {
    private Long    fileId;
    private String  fileName;
    @JsonIgnore          // R2 내부 키는 프론트에 노출하지 않음
    private String  r2Key;
    private Long    fileSize;
    private String  mimeType;
    private String  scope;         // ALL | DEPT
    private Integer deptno;
    private String  dname;         // DEPT JOIN
    private String  uploader;      // empno 문자열 or 'admin'
    private String  uploaderName;  // 표시용 이름
    private String  createdAt;     // TO_CHAR 포맷
}
