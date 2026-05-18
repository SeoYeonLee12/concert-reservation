-- =============================================================
-- 01-dummy-places-seats.sql
-- place: 50건 / seat: ~50,000건 (place당 1,000석)
-- MySQL 8.0 / INSERT ... SELECT + recursive CTE 방식
-- =============================================================

SET autocommit = 0;
SET unique_checks = 0;
SET foreign_key_checks = 0;

-- ---------------------------------------------------------------
-- place (50건)
-- ---------------------------------------------------------------
INSERT INTO place (name, address, seat_count, created_at, updated_at, deleted_at)
WITH RECURSIVE n AS (
    SELECT 1 AS i
    UNION ALL
    SELECT i + 1 FROM n WHERE i < 50
)
SELECT
    CONCAT('공연장_', LPAD(i, 2, '0')),
    CONCAT(
        ELT(((i - 1) % 8) + 1,
            '서울특별시 강남구', '서울특별시 마포구', '서울특별시 송파구',
            '부산광역시 해운대구', '인천광역시 남동구', '대구광역시 중구',
            '광주광역시 동구', '대전광역시 유성구'
        ),
        ' ',
        CONCAT(i * 3, '번길 ', i * 7)
    ),
    1000,
    NOW(),
    NOW(),
    NULL
FROM n;

-- ---------------------------------------------------------------
-- seat (50,000건: place 50개 × 1,000석)
-- section은 A~E (200석씩), seat_no는 1~200
-- ---------------------------------------------------------------
INSERT INTO seat (place_id, seat_no, section, created_at, updated_at, deleted_at)
WITH RECURSIVE seats AS (
    SELECT 1 AS seat_idx
    UNION ALL
    SELECT seat_idx + 1 FROM seats WHERE seat_idx < 1000
)
SELECT
    p.place_id,
    LPAD(((seat_idx - 1) % 200) + 1, 3, '0'),
    ELT(FLOOR((seat_idx - 1) / 200) + 1, 'A', 'B', 'C', 'D', 'E'),
    NOW(),
    NOW(),
    NULL
FROM place p
CROSS JOIN seats
ORDER BY p.place_id, seat_idx;

COMMIT;
SET autocommit = 1;
SET unique_checks = 1;
SET foreign_key_checks = 1;
