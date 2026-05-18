-- OutboxEvent 테이블 (Day 3-2: Outbox Pattern 도입)
-- 결제 완료 이벤트를 같은 트랜잭션 내 DB에 원자적 저장 → at-least-once 보장
CREATE TABLE IF NOT EXISTS outbox_event (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    event_type      VARCHAR(255)    NOT NULL,
    aggregate_type  VARCHAR(255)    NOT NULL,
    aggregate_id    BIGINT          NOT NULL,
    payload         TEXT            NOT NULL,
    status          VARCHAR(20)     NOT NULL,   -- PENDING | PUBLISHED | FAILED
    published_at    DATETIME(6),
    created_at      DATETIME(6),
    updated_at      DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_outbox_status_created (status, created_at)  -- OutboxRetryScheduler 조회 최적화
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
