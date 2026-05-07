-- =============================================================
-- 02-dummy-performances-schedules.sql
-- performance: 1,000건 / schedule: 5,000건 (공연당 5회차)
-- MySQL 8.0 / recursive CTE + cross join
-- =============================================================

SET autocommit = 0;
SET unique_checks = 0;
SET foreign_key_checks = 0;

-- ---------------------------------------------------------------
-- performance (1,000건)
-- BOOKING 60% (1~600), READY 30% (601~900), CLOSED 10% (901~1000)
-- ---------------------------------------------------------------
INSERT INTO performance (
    title, description, poster_image, running_time,
    age_rating, performer, performance_status,
    created_at, updated_at, deleted_at
)
WITH RECURSIVE n AS (
    SELECT 1 AS i
    UNION ALL
    SELECT i + 1 FROM n WHERE i < 1000
)
SELECT
    CONCAT(
        ELT(((i - 1) % 10) + 1,
            '뮤지컬', '콘서트', '오페라', '발레', '클래식',
            '재즈', '록페스티벌', '팝콘서트', '국악', '연극'
        ),
        ' ',
        ELT(((i - 1) % 20) + 1,
            '그대에게', '별빛 아래', '불꽃처럼', '봄날의 꿈',
            '여름밤의 선율', '가을의 노래', '겨울왕국', '빛과 그림자',
            '영원한 사랑', '새벽의 노래', '꿈을 향해', '하늘을 날아',
            '바다의 노래', '숲속의 비밀', '도심의 밤', '달빛 아래',
            '시간의 여행', '마음의 소리', '기억의 조각', '희망의 빛'
        ),
        ' ',
        i
    ),
    CONCAT('공연 설명 텍스트 - 작품 번호 ', i, '입니다. 감동적인 무대와 화려한 퍼포먼스를 선보입니다.'),
    CONCAT('https://cdn.example.com/poster/', i, '.jpg'),
    90 + ((i % 7) * 15),   -- 90~180분
    ELT((i % 4) + 1, '전체관람가', '12세 이상', '15세 이상', '18세 이상'),
    CONCAT(
        ELT(((i - 1) % 15) + 1,
            '김민준', '이서연', '박지호', '최예린', '정승현',
            '강하늘', '윤지수', '임도현', '한소희', '오세훈',
            '류준열', '신혜선', '이준기', '박보영', '공유'
        ),
        ' & 앙상블'
    ),
    CASE
        WHEN i <= 600  THEN 'BOOKING'
        WHEN i <= 900  THEN 'READY'
        ELSE               'CLOSED'
    END,
    DATE_ADD('2025-06-01', INTERVAL (i % 180) DAY),
    DATE_ADD('2025-06-01', INTERVAL (i % 180) DAY),
    NULL
FROM n;

-- ---------------------------------------------------------------
-- schedule (5,000건: performance 1,000개 × 5회차)
-- start_datetime: 2026-01-01 ~ 2026-12-31 분포
-- place는 place_id 1~50 순환
-- total_seats / available_seats: 200석 고정
-- ---------------------------------------------------------------
INSERT INTO schedule (
    performance_id, place_id,
    start_datetime, end_datetime, reservation_start_at,
    total_seats, available_seats,
    created_at, updated_at, deleted_at
)
WITH RECURSIVE rounds AS (
    SELECT 1 AS round_no
    UNION ALL
    SELECT round_no + 1 FROM rounds WHERE round_no < 5
)
SELECT
    p.performance_id,
    ((p.performance_id + rounds.round_no - 2) % 50) + 1  AS place_id,
    DATE_ADD(
        DATE_ADD('2026-01-01', INTERVAL FLOOR((p.performance_id - 1) / 3) DAY),
        INTERVAL ((rounds.round_no - 1) * 3 + 14) HOUR
    )  AS start_datetime,
    DATE_ADD(
        DATE_ADD(
            DATE_ADD('2026-01-01', INTERVAL FLOOR((p.performance_id - 1) / 3) DAY),
            INTERVAL ((rounds.round_no - 1) * 3 + 14) HOUR
        ),
        INTERVAL 2 HOUR
    )  AS end_datetime,
    DATE_SUB(
        DATE_ADD(
            DATE_ADD('2026-01-01', INTERVAL FLOOR((p.performance_id - 1) / 3) DAY),
            INTERVAL ((rounds.round_no - 1) * 3 + 14) HOUR
        ),
        INTERVAL 7 DAY
    )  AS reservation_start_at,
    200  AS total_seats,
    CASE
        WHEN p.performance_status = 'BOOKING' THEN FLOOR(200 * (0.3 + (p.performance_id % 5) * 0.1))
        WHEN p.performance_status = 'READY'   THEN 200
        ELSE                                       0
    END  AS available_seats,
    NOW(),
    NOW(),
    NULL
FROM performance p
CROSS JOIN rounds
ORDER BY p.performance_id, rounds.round_no;

COMMIT;
SET autocommit = 1;
SET unique_checks = 1;
SET foreign_key_checks = 1;
