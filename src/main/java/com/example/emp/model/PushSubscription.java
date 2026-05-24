package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PushSubscription {
    private String endpoint;
    private String p256dh;
    private String auth;
    private Integer empno;  // 구독한 사원 번호 (관리자는 null)
}
