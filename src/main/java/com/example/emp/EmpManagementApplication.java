package com.example.emp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

// UserDetailsServiceAutoConfiguration 제외: 우리는 JWT 방식이므로 기본 UserDetailsService 불필요
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableScheduling
@MapperScan("com.example.emp.mapper")
public class EmpManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmpManagementApplication.class, args);
    }
}
