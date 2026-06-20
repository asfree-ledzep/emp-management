package com.example.emp.service;

import com.example.emp.mapper.MsgMapper;
import com.example.emp.model.MsgFile;
import com.example.emp.model.MsgMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class MsgService {

    @Autowired private MsgMapper   msgMapper;
    @Autowired private S3Service   s3Service;

    /* ─── 받은 쪽지 목록 ─────────────────────── */
    public List<MsgMessage> getInbox(int empno) {
        return msgMapper.findInbox(empno);
    }

    /* ─── 보낸 쪽지 목록 ─────────────────────── */
    public List<MsgMessage> getSent(int empno) {
        return msgMapper.findSent(empno);
    }

    /* ─── 쪽지 상세 + 읽음 처리 ──────────────── */
    public MsgMessage getDetail(long msgId, int myEmpno) {
        MsgMessage msg = msgMapper.findById(msgId);
        if (msg == null) return null;

        // 수신자가 조회하면 읽음 처리
        if (msg.getReceiverEmpno() != null
                && msg.getReceiverEmpno().equals(myEmpno)) {
            msgMapper.markRead(msgId, myEmpno);
            if (msg.getReadAt() == null) {
                msg.setReadAt("방금"); // 화면 즉시 반영용
            }
        }
        // 첨부파일 목록 추가
        msg.setFiles(msgMapper.findFiles(msgId));
        return msg;
    }

    /* ─── 쪽지 발송 ──────────────────────────── */
    @Transactional
    public void send(int senderEmpno,
                     String title,
                     String content,
                     int receiverEmpno,
                     Long parentMsgId,
                     MultipartFile[] files) throws IOException {

        MsgMessage msg = new MsgMessage();
        msg.setSenderEmpno(senderEmpno);
        msg.setReceiverEmpno(receiverEmpno);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setParentMsgId(parentMsgId);
        msgMapper.insertMsg(msg);

        // 시퀀스 currval로 msg_id 조회
        // insertMsg 후 MSG_SEQ.CURRVAL 을 가져오기 위해 별도 쿼리
        // → Service 레벨에서 currval 사용
        long newMsgId = getLastMsgId();

        if (files != null) {
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;
                String url = s3Service.uploadMsgFile(senderEmpno, f);
                MsgFile mf = new MsgFile();
                mf.setMsgId(newMsgId);
                mf.setFileName(f.getOriginalFilename() != null ? f.getOriginalFilename() : "file");
                mf.setS3Url(url);
                mf.setFileSize(f.getSize());
                mf.setContentType(f.getContentType());
                msgMapper.insertFile(mf);
            }
        }
    }

    /* ─── 삭제 (소프트) ──────────────────────── */
    public void delete(long msgId, int myEmpno) {
        MsgMessage msg = msgMapper.findById(msgId);
        if (msg == null) return;
        if (msg.getReceiverEmpno() != null && msg.getReceiverEmpno().equals(myEmpno)) {
            msgMapper.softDeleteByReceiver(msgId, myEmpno);
        }
        if (msg.getSenderEmpno() != null && msg.getSenderEmpno().equals(myEmpno)) {
            msgMapper.softDeleteBySender(msgId, myEmpno);
        }
    }

    /* ─── 미읽음 수 ──────────────────────────── */
    public int countUnread(int empno) {
        return msgMapper.countUnread(empno);
    }

    /* ─── currval 조회 (insertMsg 직후) ──────── */
    private long getLastMsgId() {
        // MsgMapper에 별도 select 메서드를 추가하거나 SelectKey를 사용
        // 여기서는 mapper에서 직접 조회
        return msgMapper.lastMsgId();
    }
}
