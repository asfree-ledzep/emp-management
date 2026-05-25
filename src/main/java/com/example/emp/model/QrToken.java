package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class QrToken {
    private Long   tokenId;
    private String token;
    private String expiresAt; // TO_CHAR 포맷
    private String createdAt;
}
