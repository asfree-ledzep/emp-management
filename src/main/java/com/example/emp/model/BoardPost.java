package com.example.emp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class BoardPost {
    private Long    postId;
    private Integer deptno;      // null = 전체게시판, 값 있음 = 부서게시판
    private Integer empno;
    private String  authorName;
    private String  title;
    private String  content;
    private String  fileUrl;
    private String  fileName;
    private String  createdAt;
}
