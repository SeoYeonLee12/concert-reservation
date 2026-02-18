package com.example.concertreservation.user.domain;

import com.example.concertreservation.pointHistory.domain.PointHistory;
import com.example.concertreservation.pointHistory.domain.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointCharge {

    private final PointHistoryRepository pointHistoryRepository;

    public Long chargedPoint(User user, Long addedPoint) {
        user.chargedPoint(addedPoint);
        Long currentPoint = user.getPoint() + addedPoint;
        PointHistory newChargeHistory = PointHistory.chargeHistory(user, addedPoint, currentPoint);
        pointHistoryRepository.save(newChargeHistory);
        return user.getUserId();
    }
}
