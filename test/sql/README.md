# 더미 데이터 적재 가이드

## 파일 구성

| 파일 | 대상 테이블 | 예상 행 수 |
|------|------------|-----------|
| `01-dummy-places-seats.sql` | `place`, `seat` | 50 + 50,000 |
| `02-dummy-performances-schedules.sql` | `performance`, `schedule` | 1,000 + 5,000 |
| `03-dummy-performance-seats.sql` | `performance_seat` | 1,000,000 |

---

## 사전 조건

- MySQL 8.0 이상
- 로컬 인프라 기동: `docker compose -f docker-compose-local.yml up -d`
- 데이터베이스 및 테이블이 이미 생성되어 있어야 합니다 (Hibernate DDL 또는 Flyway 실행 후).

---

## 적재 순서

외래 키 의존 순서를 반드시 지켜야 합니다.

```bash
# 환경 변수 설정 (필요에 따라 변경)
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=concert_reservation
DB_USER=root
DB_PASS=password

# 1단계: place, seat
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS $DB_NAME \
  < 01-dummy-places-seats.sql

# 2단계: performance, schedule
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS $DB_NAME \
  < 02-dummy-performances-schedules.sql

# 3단계: performance_seat (100만건 — 가장 오래 걸림)
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS $DB_NAME \
  < 03-dummy-performance-seats.sql
```

### Docker Compose 컨테이너 직접 실행

```bash
docker exec -i concert-db mysql -uroot -ppassword concert_reservation \
  < 01-dummy-places-seats.sql

docker exec -i concert-db mysql -uroot -ppassword concert_reservation \
  < 02-dummy-performances-schedules.sql

docker exec -i concert-db mysql -uroot -ppassword concert_reservation \
  < 03-dummy-performance-seats.sql
```

---

## 예상 소요 시간

| 파일 | 예상 시간 | 비고 |
|------|----------|------|
| `01-*` | < 10초 | 50,050건 |
| `02-*` | < 30초 | 6,000건 |
| `03-*` | 3~10분 | 100만건, `unique_checks=0` 적용으로 단축 |

> 환경(디스크 I/O, MySQL `innodb_buffer_pool_size`)에 따라 차이 있음.  
> `innodb_buffer_pool_size`를 최소 512M 이상으로 설정하면 `03-*` 적재 시간이 절반 이상 단축됩니다.

---

## 적재 후 행 수 검증

```sql
-- 각 테이블 행 수 확인
SELECT 'place'           AS tbl, COUNT(*) AS cnt FROM place
UNION ALL
SELECT 'seat',            COUNT(*) FROM seat
UNION ALL
SELECT 'performance',     COUNT(*) FROM performance
UNION ALL
SELECT 'schedule',        COUNT(*) FROM schedule
UNION ALL
SELECT 'performance_seat',COUNT(*) FROM performance_seat;

-- 기대값:
-- place            |    50
-- seat             | 50000
-- performance      |  1000
-- schedule         |  5000
-- performance_seat | 1000000

-- performance_status 분포 확인
SELECT performance_status, COUNT(*) FROM performance GROUP BY performance_status;
-- BOOKING: 600, READY: 300, CLOSED: 100

-- performance_seat status 분포 확인
SELECT status, COUNT(*) FROM performance_seat GROUP BY status;
-- AVAILABLE: ~700,000 / TEMPORARY: ~50,000 / SOLD: ~250,000
```

---

## 주의 사항

- 03번 파일은 `CREATE TEMPORARY TABLE`과 `ROW_NUMBER()` 윈도우 함수를 사용합니다. MySQL 8.0 미만에서는 동작하지 않습니다.
- `reserved_at`이 `NOT NULL`인 제약이 있으므로, AVAILABLE 상태 좌석은 `'2999-12-31'` 임시값이 삽입됩니다. 애플리케이션 로직에서 `AVAILABLE` 상태일 때 `reserved_at`을 무시하도록 처리되어 있으면 문제 없습니다.
- 이미 데이터가 있는 상태에서 재실행하면 `AUTO_INCREMENT` 충돌이 발생할 수 있습니다. 재실행 전 `TRUNCATE TABLE` 후 실행하세요.

```sql
-- 재실행 전 초기화 (순서 중요)
SET foreign_key_checks = 0;
TRUNCATE TABLE performance_seat;
TRUNCATE TABLE schedule;
TRUNCATE TABLE performance;
TRUNCATE TABLE seat;
TRUNCATE TABLE place;
SET foreign_key_checks = 1;
```
