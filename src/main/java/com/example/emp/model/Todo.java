package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
public class Todo {
    private Long      todoId;
    private Integer   empno;
    private String    content;
    private Boolean   isDone;
    private String    priority;   // HIGH / MEDIUM / LOW
    private LocalDate dueDate;
    private Integer   sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
