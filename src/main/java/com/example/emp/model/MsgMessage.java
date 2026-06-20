package com.example.emp.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MsgMessage {
    private Long    msgId;
    private Integer senderEmpno;
    private Integer receiverEmpno;
    private String  senderName;
    private String  receiverName;
    private String  senderPhoto;
    private String  receiverPhoto;
    private String  title;
    private String  content;        // 목록에서는 미리보기(200자), 상세에서는 전체
    private String  sentAt;         // TO_CHAR 포맷 문자열
    private String  readAt;
    private boolean senderDeleted;
    private boolean recvDeleted;
    private Long    parentMsgId;
    private int     fileCount;      // 첨부파일 수 (목록용)
    private List<MsgFile> files;    // 상세 조회 시 첨부파일 목록
}
