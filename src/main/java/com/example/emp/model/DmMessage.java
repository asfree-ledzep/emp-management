package com.example.emp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DmMessage {
    private Long    dmId;
    private Integer senderEmpno;
    private Integer receiverEmpno;
    private String  senderName;
    private String  content;
    private String  sentAt;   // HH:mm
}
