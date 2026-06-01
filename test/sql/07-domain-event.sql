-- DomainEvent 테이블 (2026-05-29: OutboxEvent → DomainEvent 리팩터링)
-- Single Table Inheritance: 모든 이벤트 타입을 단일 테이블로 관리
-- uuid: Kafka message key + Consumer Redis 멱등성 키 원천
-- status: INIT | PRODUCE_SUCCESS | PRODUCE_FAIL

DROP TABLE IF EXISTS outbox_event;

CREATE TABLE IF NOT EXISTS domain_event (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    event_type      VARCHAR(50)     NOT NULL,           -- @DiscriminatorColumn (PAYMENT_CONFIRMED, ...)
    uuid            VARCHAR(36)     NOT NULL UNIQUE,    -- UUID v4, Kafka key + 멱등성 키
    status          VARCHAR(20)     NOT NULL,           -- INIT | PRODUCE_SUCCESS | PRODUCE_FAIL
    target_domain_id BIGINT         NOT NULL,           -- Aggregate Root ID (reservationId 등)
    fail_reason     TEXT,                               -- 발행 실패 사유
    -- PaymentConfirmedDomainEvent 전용 컬럼 (Single Table Inheritance)
    user_id         BIGINT,
    price           INT,
    payload         TEXT,                               -- 발행 시점 JSON 스냅샷
    -- audit
    created_at      DATETIME(6),
    updated_at      DATETIME(6),
    deleted_at      DATETIME(6),                        -- SoftDelete
    PRIMARY KEY (id),
    INDEX idx_domain_event_status_created (status, created_at),  -- RetryScheduler 조회
    INDEX idx_domain_event_uuid (uuid)                           -- Consumer 멱등성 조회용
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
