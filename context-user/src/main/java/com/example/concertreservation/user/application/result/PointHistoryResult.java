package com.example.concertreservation.user.application.result;

import com.example.concertreservation.pointHistory.domain.PointHistory;
import java.time.LocalDateTime;

public record PointHistoryResult(
        String historyType,
        Long changeAmount,
        Long currentPoint,
        LocalDateTime createdAt
) {

    public static PointHistoryResult from(PointHistory pointHistory) {
        return new PointHistoryResult(
                pointHistory.getCollectType().toString(),
                pointHistory.getAmount(),
                pointHistory.getCurrentPoint(),
                pointHistory.getCreatedAt()
        );
    }
}
