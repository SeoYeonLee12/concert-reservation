package com.example.concertreservation.waiting.application;

import com.example.concertreservation.global.error.errorcode.WaitingQueueErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import com.example.concertreservation.waiting.domain.WaitingQueue;
import com.example.concertreservation.waiting.domain.WaitingQueueRepository;
import com.example.concertreservation.waiting.domain.enums.WaitingQueueStatus;
import com.example.concertreservation.waiting.presentation.dto.WaitingQueueEnterResponse;
import com.example.concertreservation.waiting.presentation.dto.WaitingQueueStatusResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WaitingQueueService {

    private final WaitingQueueRepository waitingQueueRepository;

    @Transactional
    public WaitingQueueEnterResponse enter(Long userId, Long performanceSeatId) {
        boolean alreadyInQueue = waitingQueueRepository.existsByUserIdAndPerformanceSeatIdAndStatusIn(
                userId, performanceSeatId,
                List.of(WaitingQueueStatus.WAITING, WaitingQueueStatus.ACTIVE));
        if (alreadyInQueue) {
            throw new GlobalException(WaitingQueueErrorCode.WAITING_QUEUE_ALREADY_EXISTS);
        }

        Integer nextPosition = waitingQueueRepository.nextQueuePosition(performanceSeatId);
        WaitingQueue waitingQueue = new WaitingQueue(userId, performanceSeatId, nextPosition);
        waitingQueueRepository.save(waitingQueue);

        return new WaitingQueueEnterResponse(
                waitingQueue.getWaitingQueueId(),
                waitingQueue.getQueuePosition(),
                waitingQueue.getStatus().name());
    }

    @Transactional(readOnly = true)
    public WaitingQueueStatusResponse getStatus(Long waitingQueueId) {
        WaitingQueue waitingQueue = waitingQueueRepository.getByWaitingQueueId(waitingQueueId);

        long ahead = waitingQueueRepository.findAllByStatusOrderByQueuePositionAsc(WaitingQueueStatus.WAITING)
                .stream()
                .filter(wq -> wq.getQueuePosition() < waitingQueue.getQueuePosition())
                .count();

        return new WaitingQueueStatusResponse(
                waitingQueue.getWaitingQueueId(),
                waitingQueue.getStatus().name(),
                waitingQueue.getQueuePosition(),
                (int) ahead,
                waitingQueue.getStatus() == WaitingQueueStatus.ACTIVE);
    }

    @Transactional
    public void markDone(Long userId, Long performanceSeatId) {
        waitingQueueRepository.findByUserIdAndPerformanceSeatIdAndStatus(
                        userId, performanceSeatId, WaitingQueueStatus.ACTIVE)
                .ifPresent(WaitingQueue::done);
    }
}
