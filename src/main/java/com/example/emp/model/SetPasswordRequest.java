package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetPasswordRequest {
    private Integer empno;
    private String  password;
}
