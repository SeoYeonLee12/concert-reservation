-- ============================================================
-- Concert Reservation: Performance Index Migration
-- Target: MySQL 8.0 / InnoDB
-- Prerequisite: 01,02,03 더미 데이터 적재 완료 (place 50, seat 50k,
--               performance 1k, schedule 5k, performance_seat 1M)
-- ============================================================

-- ------------------------------------------------------------
-- [선택] 적용 전 EXPLAIN baseline 캡처
-- 주석 해제 후 실행하여 type / rows / Extra 값 기록
-- ------------------------------------------------------------
/*
-- Q1: 공연 목록 조회 (DISTINCT + ORDER BY, filesort 여부 확인)
EXPLAIN SELECT DISTINCT p.*
FROM performance p
JOIN schedule s ON s.performance_id = p.performance_id AND s.deleted_at IS NULL
JOIN place pl ON pl.place_id = s.place_id AND pl.deleted_at IS NULL
WHERE p.deleted_at IS NULL
ORDER BY p.performance_id DESC, s.start_datetime ASC;

-- Q3: 스케줄 조회 (FK 단일 인덱스 + ORDER BY filesort 여부 확인)
EXPLAIN SELECT s.*
FROM schedule s
WHERE s.performance_id = 1 AND s.deleted_at IS NULL
ORDER BY s.start_datetime ASC;

-- Q4: 좌석 배치도 조회 (DISP-04 시나리오, schedule_id range scan)
EXPLAIN SELECT ps.*
FROM performance_seat ps
WHERE ps.schedule_id = 100 AND ps.deleted_at IS NULL
ORDER BY ps.seat_id;

-- Q5: TEMPORARY 좌석 만료 처리 (스케줄러 배치)
EXPLAIN SELECT ps.*
FROM performance_seat ps
WHERE ps.status = 'TEMPORARY' AND ps.reserved_at < NOW() AND ps.deleted_at IS NULL;
*/

-- ------------------------------------------------------------
-- 인덱스 생성 (멱등성 보장 — Stored Procedure 방식)
-- MySQL 8.0은 CREATE INDEX IF NOT EXISTS 미지원이므로
-- information_schema 확인 후 동적 생성하여 1061 오류를 회피합니다.
-- ------------------------------------------------------------

DROP PROCEDURE IF EXISTS cr_create_index_if_not_exists;

DELIMITER $$

CREATE PROCEDURE cr_create_index_if_not_exists(
    IN p_table_name  VARCHAR(64),
    IN p_index_name  VARCHAR(64),
    IN p_index_cols  VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_table_name
          AND INDEX_NAME   = p_index_name
    ) THEN
        SET @ddl = CONCAT(
            'CREATE INDEX ', p_index_name,
            ' ON ', p_table_name, ' (', p_index_cols, ')',
            ' ALGORITHM=INPLACE LOCK=NONE'
        );
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

-- Q1 ORDER BY filesort 회피 + Q3 lookup
-- InnoDB FK 자동 인덱스 schedule(performance_id) 단독은 그대로 유지하되
-- 복합 인덱스를 추가하여 start_datetime 정렬까지 커버합니다.
CALL cr_create_index_if_not_exists(
    'schedule',
    'idx_schedule_perf_start',
    'performance_id, start_datetime'
);

-- Q4 좌석 배치도 조회: schedule_id eq-lookup 후 status 필터
CALL cr_create_index_if_not_exists(
    'performance_seat',
    'idx_perf_seat_schedule_status',
    'schedule_id, status'
);

-- Q5 TEMPORARY 만료 스캐너: status eq-lookup 후 reserved_at 범위 스캔
CALL cr_create_index_if_not_exists(
    'performance_seat',
    'idx_perf_seat_status_reserved',
    'status, reserved_at'
);

DROP PROCEDURE IF EXISTS cr_create_index_if_not_exists;

-- ------------------------------------------------------------
-- [선택] 적용 후 EXPLAIN 캡처 (동일 쿼리 재실행하여 after 기록)
-- ------------------------------------------------------------
/*
EXPLAIN SELECT DISTINCT p.*
FROM performance p
JOIN schedule s ON s.performance_id = p.performance_id AND s.deleted_at IS NULL
JOIN place pl ON pl.place_id = s.place_id AND pl.deleted_at IS NULL
WHERE p.deleted_at IS NULL
ORDER BY p.performance_id DESC, s.start_datetime ASC;

EXPLAIN SELECT s.*
FROM schedule s
WHERE s.performance_id = 1 AND s.deleted_at IS NULL
ORDER BY s.start_datetime ASC;

EXPLAIN SELECT ps.*
FROM performance_seat ps
WHERE ps.schedule_id = 100 AND ps.deleted_at IS NULL
ORDER BY ps.seat_id;

EXPLAIN SELECT ps.*
FROM performance_seat ps
WHERE ps.status = 'TEMPORARY' AND ps.reserved_at < NOW() AND ps.deleted_at IS NULL;
*/

-- ------------------------------------------------------------
-- 롤백 (필요 시 주석 해제)
-- ------------------------------------------------------------
/*
DROP INDEX idx_schedule_perf_start       ON schedule;
DROP INDEX idx_perf_seat_schedule_status ON performance_seat;
DROP INDEX idx_perf_seat_status_reserved ON performance_seat;
*/
