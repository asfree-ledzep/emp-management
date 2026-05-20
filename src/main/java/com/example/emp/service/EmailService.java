package com.example.emp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

@Service
public class EmailService {

    private final SesV2Client sesClient;
    private final String from;
    private final String to;

    public EmailService(@Value("${ses.from}") String from,
                        @Value("${ses.to}")   String to,
                        @Value("${ses.region}") String region) {
        this.from = from;
        this.to   = to;
        this.sesClient = SesV2Client.builder()
                .region(Region.of(region))
                .build();
    }

    // 사원 등록 알림
    public void sendCreateNotification(int empno, String ename) {
        String subject = "[사원관리] 새 사원 등록 알림";
        String body = String.format(
                "안녕하세요.\n\n새 사원이 등록되었습니다.\n\n" +
                "  사번  : %d\n" +
                "  이름  : %s\n\n" +
                "사원 관리 시스템에서 확인하세요.",
                empno, ename);
        send(subject, body);
    }

    // 사원 삭제 알림
    public void sendDeleteNotification(int empno, String ename) {
        String subject = "[사원관리] 사원 삭제 알림";
        String body = String.format(
                "안녕하세요.\n\n사원이 삭제되었습니다.\n\n" +
                "  사번  : %d\n" +
                "  이름  : %s\n\n" +
                "사원 관리 시스템에서 확인하세요.",
                empno, ename);
        send(subject, body);
    }

    private void send(String subject, String body) {
        sesClient.sendEmail(SendEmailRequest.builder()
                .fromEmailAddress(from)
                .destination(Destination.builder().toAddresses(to).build())
                .content(EmailContent.builder()
                        .simple(Message.builder()
                                .subject(Content.builder().data(subject).charset("UTF-8").build())
                                .body(Body.builder()
                                        .text(Content.builder().data(body).charset("UTF-8").build())
                                        .build())
                                .build())
                        .build())
                .build());
    }
}
