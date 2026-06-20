package com.example.emp.mapper;

import com.example.emp.model.MsgFile;
import com.example.emp.model.MsgMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MsgMapper {

    /* ─── 발송 ─────────────────────────────── */
    void insertMsg(MsgMessage msg);
    void insertFile(MsgFile file);

    /* ─── 목록 ─────────────────────────────── */
    List<MsgMessage> findInbox(@Param("empno") int empno);
    List<MsgMessage> findSent(@Param("empno") int empno);

    /* ─── 상세 ─────────────────────────────── */
    MsgMessage    findById(@Param("msgId") long msgId);
    List<MsgFile> findFiles(@Param("msgId") long msgId);

    /* ─── 읽음 처리 ─────────────────────────── */
    void markRead(@Param("msgId") long msgId, @Param("empno") int empno);

    /* ─── 소프트 삭제 ───────────────────────── */
    void softDeleteByReceiver(@Param("msgId") long msgId, @Param("empno") int empno);
    void softDeleteBySender  (@Param("msgId") long msgId, @Param("empno") int empno);

    /* ─── 미읽음 수 ─────────────────────────── */
    int countUnread(@Param("empno") int empno);

    /* ─── CURRVAL 조회 (insertMsg 직후 호출) ── */
    long lastMsgId();

    /* ─── 받는 사람 검색 (직원 목록) ─────────── */
    List<MsgMessage> findRecipientList();
}
