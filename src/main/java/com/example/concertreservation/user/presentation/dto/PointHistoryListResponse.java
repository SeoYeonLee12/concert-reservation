package com.example.concertreservation.user.presentation.dto;

import com.example.concertreservation.user.application.result.PointHistoryResult;
import java.util.List;

public record PointHistoryListResponse(
        List<PointHistoryResponse> pointHistories
) {

    public static PointHistoryListResponse from(List<PointHistoryResult> results) {
        List<PointHistoryResponse> responses = results.stream()
                .map(PointHistoryResponse::from)
                .toList();
        return new PointHistoryListResponse(responses);
    }
}
