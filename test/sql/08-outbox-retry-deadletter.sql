-- Outbox 재처리 개선 (2026-06-01)
-- domain_event: retry_count 컬럼 추가
-- dead_letter: 최대 재시도 초과 이벤트 기록 테이블 신규 생성

ALTER TABLE domain_event
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0 AFTER fail_reason;
-- status 허용값: INIT | PRODUCE_SUCCESS | PRODUCE_FAIL | ABANDONED

CREATE TABLE IF NOT EXISTS dead_letter (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    uuid             VARCHAR(36)   NOT NULL,            -- 원본 DomainEvent.uuid
    domain_event_id  BIGINT        NOT NULL,            -- 느슨한 참조 (FK 제약 없음)
    fail_reason      TEXT,                              -- 최종 실패 사유
    recovered        BOOLEAN       NOT NULL DEFAULT FALSE, -- 운영자 수동 복구 여부
    created_at       DATETIME(6),
    updated_at       DATETIME(6),
    deleted_at       DATETIME(6),                       -- SoftDelete
    PRIMARY KEY (id),
    INDEX idx_dead_letter_recovered (recovered),        -- 복구 대기 조회
    INDEX idx_dead_letter_uuid (uuid)                   -- uuid 기반 조회
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
