package com.example.emp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 앱 시작 시 필수 테이블 자동 생성
 * - 테이블이 이미 있으면 무시 (ORA-00955)
 * - 로컬 Docker Oracle / AWS EB Oracle 모두 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        createEmpAuth();
    }

    private void createEmpAuth() {
        try {
            // 이미 있는지 확인
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_tables WHERE table_name = 'EMP_AUTH'",
                Integer.class
            );
            if (count != null && count > 0) {
                log.info("[SchemaInit] EMP_AUTH 테이블 이미 존재");
                return;
            }
            // 없으면 생성
            jdbc.execute(
                "CREATE TABLE EMP_AUTH (" +
                "    EMPNO      NUMBER        NOT NULL, " +
                "    PASSWORD   VARCHAR2(100) NOT NULL, " +
                "    CREATED_AT DATE          DEFAULT SYSDATE, " +
                "    CONSTRAINT PK_EMP_AUTH PRIMARY KEY (EMPNO), " +
                "    CONSTRAINT FK_EMP_AUTH_EMPNO FOREIGN KEY (EMPNO) REFERENCES EMP(EMPNO)" +
                ")"
            );
            log.info("[SchemaInit] EMP_AUTH 테이블 생성 완료");
        } catch (Exception e) {
            log.warn("[SchemaInit] EMP_AUTH 테이블 생성 실패 (무시): {}", e.getMessage());
        }
    }
}
