-- =============================================================
-- 03-dummy-performance-seats.sql
-- performance_seat: 1,000,000건
--   schedule 5,000개 × 200석 = 1,000,000
--   status 비율: AVAILABLE 70%, TEMPORARY 5%, SOLD 25%
--   seat는 schedule의 place에 속한 seat 중 1~200번 순환
-- MySQL 8.0 / INSERT ... SELECT + recursive CTE
-- =============================================================

SET autocommit = 0;
SET unique_checks = 0;
SET foreign_key_checks = 0;

-- ---------------------------------------------------------------
-- numbers(1~200) CTE를 이용해 schedule × seat_offset 카르테시안 곱
-- seat_id는 해당 place의 seat 중 offset 순서(row_number)로 매핑
-- ---------------------------------------------------------------

-- 1단계: place별 seat에 순번 부여 (임시 테이블)
CREATE TEMPORARY TABLE IF NOT EXISTS tmp_place_seat_rank AS
SELECT
    s.seat_id,
    s.place_id,
    ROW_NUMBER() OVER (PARTITION BY s.place_id ORDER BY s.seat_id) AS seat_rank
FROM seat s;

CREATE INDEX idx_tmp_psr ON tmp_place_seat_rank (place_id, seat_rank);

-- 2단계: 1,000,000건 INSERT
INSERT INTO performance_seat (
    schedule_id, seat_id,
    price, status,
    reserved_at, version,
    created_at, updated_at, deleted_at
)
WITH RECURSIVE offsets AS (
    SELECT 1 AS seat_offset
    UNION ALL
    SELECT seat_offset + 1 FROM offsets WHERE seat_offset < 200
)
SELECT
    sc.schedule_id,
    ts.seat_id,
    -- price: 30,000 ~ 200,000 (10,000 단위 18단계)
    (FLOOR(
        (CRC32(CONCAT(sc.schedule_id, '-', offsets.seat_offset)) % 18)
    ) + 3) * 10000  AS price,
    -- status: AVAILABLE 70%, TEMPORARY 5%, SOLD 25%
    -- CRC32 mod 20: 0~13=AVAILABLE, 14=TEMPORARY, 15~19=SOLD
    ELT(
        CASE
            WHEN (CRC32(CONCAT('s', sc.schedule_id, 'o', offsets.seat_offset)) % 20) <= 13 THEN 1
            WHEN (CRC32(CONCAT('s', sc.schedule_id, 'o', offsets.seat_offset)) % 20) = 14  THEN 2
            ELSE                                                                                  3
        END,
        'AVAILABLE', 'TEMPORARY', 'SOLD'
    )  AS status,
    -- reserved_at: SOLD/TEMPORARY는 실제 예약 시각, AVAILABLE은 임의 과거값
    CASE
        WHEN (CRC32(CONCAT('s', sc.schedule_id, 'o', offsets.seat_offset)) % 20) >= 14
            THEN DATE_ADD('2026-01-01',
                     INTERVAL FLOOR(CRC32(CONCAT('r', sc.schedule_id, offsets.seat_offset)) % 365) DAY)
        ELSE '2999-12-31 00:00:00'
    END  AS reserved_at,
    0    AS version,
    NOW(),
    NOW(),
    NULL
FROM schedule sc
CROSS JOIN offsets
JOIN tmp_place_seat_rank ts
    ON ts.place_id = sc.place_id
   AND ts.seat_rank = offsets.seat_offset;

DROP TEMPORARY TABLE IF EXISTS tmp_place_seat_rank;

COMMIT;
SET autocommit = 1;
SET unique_checks = 1;
SET foreign_key_checks = 1;
