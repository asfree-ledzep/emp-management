package com.example.emp.model;

import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Getter
@Setter
public class EmpAuth {
    private Integer empno;
    private String  password;
    private Date    createdAt;
}
