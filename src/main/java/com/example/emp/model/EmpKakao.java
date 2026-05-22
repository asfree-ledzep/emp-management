package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmpKakao {
    private Integer empno;
    private String accessToken;
    private String refreshToken;
}
