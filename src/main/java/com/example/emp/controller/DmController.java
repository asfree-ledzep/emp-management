package com.example.emp.controller;

import com.example.emp.mapper.DmMapper;
import com.example.emp.mapper.EmpMapper;
import com.example.emp.model.DmMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 1:1 DM 채팅 컨트롤러
 *
 * WebSocket: /app/dm.send → DB 저장 → /topic/dm/{receiverEmpno} + /topic/dm/{senderEmpno}
 * REST: GET /api/dm/history        — 두 사용자 간 대화 이력
 *       GET /api/employees/search  — 직원 이름 검색 (DM 상대 선택용)
 */
@Controller
public class DmController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter HHmm = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired private DmMapper              dmMapper;
    @Autowired private EmpMapper             empMapper;
    @Autowired private SimpMessagingTemplate messaging;

    /** DM 수신 → DB 저장 → 수신자·발신자 토픽 전송 */
    @MessageMapping("/dm.send")
    public void handleDm(DmMessage message) {
        message.setSentAt(LocalTime.now(KST).format(HHmm));
        dmMapper.insert(message);

        // 수신자 토픽
        messaging.convertAndSend("/topic/dm/" + message.getReceiverEmpno(), message);
        // 발신자 토픽 (자신의 채팅창 업데이트)
        if (!message.getReceiverEmpno().equals(message.getSenderEmpno())) {
            messaging.convertAndSend("/topic/dm/" + message.getSenderEmpno(), message);
        }
    }

    /** 두 사용자 간 DM 이력 조회 */
    @GetMapping("/api/dm/history")
    @ResponseBody
    public List<DmMessage> history(
            @RequestParam Integer empno1,
            @RequestParam Integer empno2,
            @RequestParam(defaultValue = "50") int limit) {
        return dmMapper.findHistory(empno1, empno2, limit);
    }

    /** 나와 대화 이력이 있는 상대 사번 목록 (최근 순) */
    @GetMapping("/api/dm/partners")
    @ResponseBody
    public List<Integer> getPartners(@RequestParam int myEmpno) {
        return dmMapper.findPartnerEmpnos(myEmpno);
    }

    /** 직원 이름 검색 (name 미입력 시 전체 반환) */
    @GetMapping("/api/employees/search")
    @ResponseBody
    public List<Map<String, Object>> searchEmps(
            @RequestParam(defaultValue = "") String name) {
        return empMapper.findAll().stream()
                .filter(e -> name.isEmpty() ||
                        (e.getEname() != null && e.getEname().contains(name)))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("empno",    e.getEmpno());
                    m.put("ename",    e.getEname()    != null ? e.getEname()    : "");
                    m.put("job",      e.getJob()      != null ? e.getJob()      : "");
                    m.put("photoUrl", e.getPhotoUrl() != null ? e.getPhotoUrl() : "");
                    return m;
                })
                .collect(Collectors.toList());
    }
}
