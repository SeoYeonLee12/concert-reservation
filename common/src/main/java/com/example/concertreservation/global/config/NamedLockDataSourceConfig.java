package com.example.concertreservation.global.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Named Lock 전용 별도 DataSource 설정.
 *
 * 문제: Named Lock(GET_LOCK/RELEASE_LOCK)은 요청당 HikariCP 연결을 2개 소비한다.
 *   연결 #1: JdbcTemplate → GET_LOCK (트랜잭션 종료까지 유지)
 *   연결 #2: @Transactional → JPA doReserve()
 * pool=10 기본값에서 동시 처리 가능 Named Lock 요청 = 5건 → 100VU 시 전량 타임아웃.
 *
 * 해결: GET_LOCK/RELEASE_LOCK 전용 DataSource를 분리해 메인 풀 고갈을 원천 차단.
 * Named Lock 전용 풀(pool=10)이 자연 쓰로틀 역할: 최대 10건만 진입, 나머지는 NamedLockPool 대기.
 * 메인 풀은 @Transactional JPA 쿼리에만 사용 → pool=10으로도 고갈 없음.
 */
@Configuration
public class NamedLockDataSourceConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Primary
    @Bean(name = "dataSource")
    public HikariDataSource primaryDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driverClassName);
        ds.setMaximumPoolSize(10);
        ds.setPoolName("MainPool");
        return ds;
    }

    @Bean(name = "namedLockDataSource")
    public HikariDataSource namedLockDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driverClassName);
        ds.setMaximumPoolSize(10);
        ds.setPoolName("NamedLockPool");
        ds.setConnectionTimeout(3000);
        return ds;
    }

    // 메인 풀 기반 JdbcTemplate — auto-configuration 백오프 대응용
    @Bean(name = "jdbcTemplate")
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(primaryDataSource());
    }

    // Named Lock 전용 JdbcTemplate — NamedLockService에서 @Qualifier로 주입
    @Bean(name = "namedLockJdbcTemplate")
    public JdbcTemplate namedLockJdbcTemplate() {
        return new JdbcTemplate(namedLockDataSource());
    }
}
