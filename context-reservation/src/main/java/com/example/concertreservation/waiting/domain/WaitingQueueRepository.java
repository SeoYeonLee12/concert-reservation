package com.example.concertreservation.waiting.domain;

import com.example.concertreservation.global.error.errorcode.WaitingQueueErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import com.example.concertreservation.waiting.domain.enums.WaitingQueueStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WaitingQueueRepository extends JpaRepository<WaitingQueue, Long> {

    Optional<WaitingQueue> findByWaitingQueueId(Long waitingQueueId);

    default WaitingQueue getByWaitingQueueId(Long id) {
        return findByWaitingQueueId(id)
                .orElseThrow(() -> new GlobalException(WaitingQueueErrorCode.WAITING_QUEUE_NOT_FOUND));
    }

    List<WaitingQueue> findAllByStatusOrderByQueuePositionAsc(WaitingQueueStatus status);

    Optional<WaitingQueue> findByUserIdAndPerformanceSeatIdAndStatus(
            Long userId, Long performanceSeatId, WaitingQueueStatus status);

    boolean existsByUserIdAndPerformanceSeatIdAndStatusIn(
            Long userId, Long performanceSeatId, List<WaitingQueueStatus> statuses);

    @Query("SELECT COALESCE(MAX(wq.queuePosition), 0) + 1 FROM WaitingQueue wq WHERE wq.performanceSeatId = :seatId")
    Integer nextQueuePosition(@Param("seatId") Long seatId);

    List<WaitingQueue> findAllByStatusAndActivatedAtBefore(
            WaitingQueueStatus status, LocalDateTime expiredBefore);
}
