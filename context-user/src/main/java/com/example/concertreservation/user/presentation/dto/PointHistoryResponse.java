package com.example.concertreservation.user.presentation.dto;

import com.example.concertreservation.user.application.result.PointHistoryResult;
import java.time.LocalDateTime;

public record PointHistoryResponse(
        String historyType,
        Long changeAmount,
        Long currentPoint,
        LocalDateTime createdAt
) {

    public static PointHistoryResponse from(PointHistoryResult pointHistoryResult) {
        return new PointHistoryResponse(
                pointHistoryResult.historyType(),
                pointHistoryResult.changeAmount(),
                pointHistoryResult.currentPoint(),
                pointHistoryResult.createdAt()
        );
    }

}
