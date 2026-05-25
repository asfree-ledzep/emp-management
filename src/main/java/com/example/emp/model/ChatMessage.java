package com.example.emp.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private Long    msgId;
    private Integer empno;
    private String  senderName;
    private String  content;
    private String  sentAt;   // HH:mm 형식 (DB: TO_CHAR(SENT_AT,'HH24:MI'))
}
