package com.example.concertreservation.waiting.domain;

import com.example.concertreservation.global.domain.BaseDomain;
import com.example.concertreservation.global.error.errorcode.WaitingQueueErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import com.example.concertreservation.waiting.domain.enums.WaitingQueueStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "waiting_queue")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WaitingQueue extends BaseDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "waiting_queue_id")
    private Long waitingQueueId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "performance_seat_id", nullable = false)
    private Long performanceSeatId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WaitingQueueStatus status;

    @Column(name = "queue_position", nullable = false)
    private Integer queuePosition;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    public WaitingQueue(Long userId, Long performanceSeatId, Integer queuePosition) {
        this.userId = userId;
        this.performanceSeatId = performanceSeatId;
        this.status = WaitingQueueStatus.WAITING;
        this.queuePosition = queuePosition;
    }

    public void activate() {
        if (this.status != WaitingQueueStatus.WAITING) {
            throw new GlobalException(WaitingQueueErrorCode.WAITING_QUEUE_INVALID_STATUS);
        }
        this.status = WaitingQueueStatus.ACTIVE;
        this.activatedAt = LocalDateTime.now();
    }

    public void done() {
        if (this.status != WaitingQueueStatus.ACTIVE) {
            throw new GlobalException(WaitingQueueErrorCode.WAITING_QUEUE_NOT_ACTIVE);
        }
        this.status = WaitingQueueStatus.DONE;
    }

    public void expire() {
        if (this.status != WaitingQueueStatus.WAITING && this.status != WaitingQueueStatus.ACTIVE) {
            throw new GlobalException(WaitingQueueErrorCode.WAITING_QUEUE_INVALID_STATUS);
        }
        this.status = WaitingQueueStatus.EXPIRED;
    }

    public boolean isExpired(LocalDateTime now, Duration window) {
        if (this.status != WaitingQueueStatus.ACTIVE || this.activatedAt == null) {
            return false;
        }
        return activatedAt.plus(window).isBefore(now);
    }
}
