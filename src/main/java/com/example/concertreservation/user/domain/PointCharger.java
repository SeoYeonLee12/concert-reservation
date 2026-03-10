package com.example.concertreservation.user.domain;

import com.example.concertreservation.pointHistory.domain.PointHistory;
import com.example.concertreservation.pointHistory.domain.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointCharger {

    private final PointHistoryRepository pointHistoryRepository;

    public Long chargePoint(User user, Long addedPoint) {
        user.chargedPoint(addedPoint);
        Long currentPoint = user.getPoint();
        PointHistory newChargeHistory = PointHistory.chargeHistory(user, addedPoint, currentPoint);
        pointHistoryRepository.save(newChargeHistory);
        return currentPoint;
    }
}
