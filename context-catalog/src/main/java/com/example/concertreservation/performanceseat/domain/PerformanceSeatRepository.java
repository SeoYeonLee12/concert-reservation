package com.example.concertreservation.performanceseat.domain;

import com.example.concertreservation.global.error.errorcode.PerformanceSeatErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    // DISP-04 좌석 배치도 조회 (schedule_id 기준)
    Page<PerformanceSeat> findAllByScheduleScheduleId(Long scheduleId, Pageable pageable);

    // 만료 좌석 일괄 조회 (5분 만료 스케줄러)
    @Query("SELECT ps FROM PerformanceSeat ps " +
            "WHERE ps.seatStatus = com.example.concertreservation.performanceseat.domain.enums.SeatStatus.TEMPORARY " +
            "AND ps.reservedAt < :expiredBefore")
    List<PerformanceSeat> findExpiredTemporarySeats(@Param("expiredBefore") LocalDateTime expiredBefore);
}
