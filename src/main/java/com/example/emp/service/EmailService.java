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
        String text = String.format(
                "새 사원이 등록되었습니다.\n사번: %d\n이름: %s", empno, ename);
        String html = buildHtml(
                "#22c55e",
                "👤 새 사원 등록 알림",
                "새로운 사원이 등록되었습니다.",
                empno, ename,
                "등록"
        );
        send(subject, text, html);
    }

    // 사원 삭제 알림
    public void sendDeleteNotification(int empno, String ename) {
        String subject = "[사원관리] 사원 삭제 알림";
        String text = String.format(
                "사원이 삭제되었습니다.\n사번: %d\n이름: %s", empno, ename);
        String html = buildHtml(
                "#ef4444",
                "🗑️ 사원 삭제 알림",
                "아래 사원이 시스템에서 삭제되었습니다.",
                empno, ename,
                "삭제"
        );
        send(subject, text, html);
    }

    // HTML 이메일 템플릿 생성
    private String buildHtml(String color, String title, String desc,
                              int empno, String ename, String actionLabel) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="ko">
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
            <body style="margin:0;padding:0;background:#f3f4f6;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;padding:40px 0;">
                <tr><td align="center">
                  <table width="520" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 16px rgba(0,0,0,0.1);">

                    <!-- 헤더 -->
                    <tr>
                      <td style="background:%s;padding:32px 40px;text-align:center;">
                        <h1 style="margin:0;color:#ffffff;font-size:1.4rem;font-weight:700;">%s</h1>
                      </td>
                    </tr>

                    <!-- 본문 -->
                    <tr>
                      <td style="padding:32px 40px;">
                        <p style="margin:0 0 24px;color:#374151;font-size:0.95rem;">%s</p>

                        <!-- 사원 정보 카드 -->
                        <table width="100%%" cellpadding="0" cellspacing="0"
                               style="background:#f9fafb;border-radius:8px;border:1px solid #e5e7eb;">
                          <tr>
                            <td style="padding:20px 24px;">
                              <table width="100%%" cellpadding="0" cellspacing="0">
                                <tr>
                                  <td style="padding:6px 0;color:#6b7280;font-size:0.85rem;font-weight:600;width:80px;">사번</td>
                                  <td style="padding:6px 0;color:#111827;font-size:0.95rem;font-weight:700;">%d</td>
                                </tr>
                                <tr>
                                  <td style="padding:6px 0;color:#6b7280;font-size:0.85rem;font-weight:600;">이름</td>
                                  <td style="padding:6px 0;color:#111827;font-size:0.95rem;font-weight:700;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:6px 0;color:#6b7280;font-size:0.85rem;font-weight:600;">처리</td>
                                  <td style="padding:6px 0;">
                                    <span style="background:%s;color:#fff;padding:2px 10px;border-radius:12px;font-size:0.8rem;font-weight:600;">%s</span>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>

                    <!-- 푸터 -->
                    <tr>
                      <td style="background:#f9fafb;padding:16px 40px;border-top:1px solid #e5e7eb;text-align:center;">
                        <p style="margin:0;color:#9ca3af;font-size:0.8rem;">사원 관리 시스템 · 자동 발송 메일입니다.</p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """,
                color, title, desc, empno, ename, color, actionLabel);
    }

    private void send(String subject, String text, String html) {
        sesClient.sendEmail(SendEmailRequest.builder()
                .fromEmailAddress(from)
                .destination(Destination.builder().toAddresses(to).build())
                .content(EmailContent.builder()
                        .simple(Message.builder()
                                .subject(Content.builder().data(subject).charset("UTF-8").build())
                                .body(Body.builder()
                                        .text(Content.builder().data(text).charset("UTF-8").build())
                                        .html(Content.builder().data(html).charset("UTF-8").build())
                                        .build())
                                .build())
                        .build())
                .build());
    }
}
