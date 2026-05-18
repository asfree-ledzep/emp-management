package com.example.emp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.emp.mapper")
public class EmpManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmpManagementApplication.class, args);
    }
}
