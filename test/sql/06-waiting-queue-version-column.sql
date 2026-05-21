-- 대기열 테이블 생성
CREATE TABLE IF NOT EXISTS waiting_queue (
    waiting_queue_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT      NOT NULL,
    performance_seat_id BIGINT   NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    queue_position   INT         NOT NULL,
    activated_at     DATETIME    NULL,
    created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_waiting_queue_seat_status (performance_seat_id, status),
    INDEX idx_waiting_queue_user_seat (user_id, performance_seat_id, status)
);

-- 낙관적 락 버전 컬럼 추가 (performance_seat)
ALTER TABLE performance_seat
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
