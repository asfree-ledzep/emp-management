package com.example.emp.service;

import com.example.emp.mapper.EmpMapper;
import com.example.emp.mapper.PushSubscriptionMapper;
import com.example.emp.model.Emp;
import com.example.emp.model.PushSubscription;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);

    @Value("${vapid.public-key}")
    private String vapidPublicKey;

    @Value("${vapid.private-key}")
    private String vapidPrivateKey;

    @Autowired private PushSubscriptionMapper subscriptionMapper;
    @Autowired private EmpMapper              empMapper;

    private nl.martijndwars.webpush.PushService webPushService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() throws Exception {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        webPushService = new nl.martijndwars.webpush.PushService(vapidPublicKey, vapidPrivateKey);
        webPushService.setSubject("mailto:admin@emp-management.com");
    }

    // 구독 저장
    public void subscribe(PushSubscription sub) {
        subscriptionMapper.upsert(sub);
    }

    // 구독 삭제
    public void unsubscribe(String endpoint) {
        subscriptionMapper.deleteByEndpoint(endpoint);
    }

    // 전체 구독자에게 푸시 발송
    public void sendToAll(String title, String body) {
        List<PushSubscription> subs = subscriptionMapper.findAll();
        if (subs.isEmpty()) {
            log.info("등록된 구독자 없음");
            return;
        }
        for (PushSubscription sub : subs) {
            sendOne(sub, title, body);
        }
    }

    private void sendOne(PushSubscription sub, String title, String body) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of("title", title, "body", body));
            Notification notification = new Notification(
                    sub.getEndpoint(),
                    sub.getP256dh(),
                    sub.getAuth(),
                    payload.getBytes(StandardCharsets.UTF_8)
            );
            webPushService.send(notification);
            log.info("푸시 발송 성공: {}", title);
        } catch (Exception e) {
            log.warn("푸시 발송 실패 [{}]: {}", sub.getEndpoint().substring(0, 30), e.getMessage());
            // 만료/삭제된 구독은 DB에서 제거
            String msg = e.getMessage();
            if (msg != null && (msg.contains("404") || msg.contains("410"))) {
                subscriptionMapper.deleteByEndpoint(sub.getEndpoint());
            }
        }
    }

    // 매일 오전 9시 (한국 시간) — 입사 기념일 체크
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void checkAnniversaries() {
        log.info("입사 기념일 체크 시작");
        List<Emp> targets = empMapper.findHiredToday();
        if (targets.isEmpty()) {
            log.info("오늘 입사 기념일인 사원 없음");
            return;
        }
        LocalDate today = LocalDate.now();
        for (Emp emp : targets) {
            int years = today.getYear() - emp.getHiredate().getYear();
            if (years <= 0) continue;
            String title = "🎉 입사 기념일 축하";
            String body  = String.format("축하합니다. %s님, 입사하신지 %d년 차 입니다.", emp.getEname(), years);
            log.info("기념일 푸시 발송: {}", body);
            sendToAll(title, body);
        }
    }
}
