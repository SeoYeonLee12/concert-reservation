package com.example.concertreservation.performanceseat.domain;

import com.example.concertreservation.global.error.errorcode.PerformanceSeatErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PerformanceSeatRepository extends JpaRepository<PerformanceSeat, Long> {

    Optional<PerformanceSeat> findByPerformanceSeatId(Long performanceSeatId);

    default PerformanceSeat getByPerformanceSeatId(Long performanceSeatId) {
        return findByPerformanceSeatId(performanceSeatId).orElseThrow(
                () -> new GlobalException(PerformanceSeatErrorCode.SEAT_NOT_FOUND));
    }

    // 비관적 락 전용 조회 — SELECT FOR UPDATE로 동일 좌석 동시 접근을 DB 레벨에서 직렬화.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ps FROM PerformanceSeat ps WHERE ps.performanceSeatId = :id")
    Optional<PerformanceSeat> findByPerformanceSeatIdWithPessimisticLock(@Param("id") Long id);

    default PerformanceSeat getByPerformanceSeatIdWithPessimisticLock(Long performanceSeatId) {
        return findByPerformanceSeatIdWithPessimisticLock(performanceSeatId).orElseThrow(
                () -> new GlobalException(PerformanceSeatErrorCode.SEAT_NOT_FOUND));
    }

    // DISP-04 좌석 배치도 조회 (schedule_id 기준) — seat JOIN FETCH로 N+1 방지
    @EntityGraph(attributePaths = {"seat"})
    Page<PerformanceSeat> findAllByScheduleScheduleId(Long scheduleId, Pageable pageable);

    // 만료 좌석 일괄 조회 (5분 만료 스케줄러)
    @Query("SELECT ps FROM PerformanceSeat ps " +
            "WHERE ps.seatStatus = com.example.concertreservation.performanceseat.domain.enums.SeatStatus.TEMPORARY " +
            "AND ps.reservedAt < :expiredBefore")
    List<PerformanceSeat> findExpiredTemporarySeats(@Param("expiredBefore") LocalDateTime expiredBefore);
}
