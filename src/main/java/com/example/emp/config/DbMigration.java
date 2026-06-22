package com.example.emp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.Statement;

@Component
public class DbMigration {

    private static final Logger log = LoggerFactory.getLogger(DbMigration.class);

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void migrate() {
        String[] sqls = {
            "ALTER TABLE EMP MODIFY SAL  NUMBER(12,2)",
            "ALTER TABLE EMP MODIFY COMM NUMBER(12,2)"
        };
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : sqls) {
                try {
                    stmt.execute(sql);
                    log.info("[DbMigration] 실행 완료: {}", sql);
                } catch (Exception e) {
                    log.warn("[DbMigration] 이미 적용됨 또는 오류 무시: {} — {}", sql, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[DbMigration] DB 연결 실패: {}", e.getMessage());
        }
    }
}
