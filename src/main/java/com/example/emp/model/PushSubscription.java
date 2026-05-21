package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PushSubscription {
    private String endpoint;
    private String p256dh;
    private String auth;
}
