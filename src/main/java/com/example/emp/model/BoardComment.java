package com.example.emp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class BoardComment {
    private Long    commentId;
    private Long    postId;
    private Integer empno;
    private String  authorName;
    private String  content;
    private String  createdAt;
}
