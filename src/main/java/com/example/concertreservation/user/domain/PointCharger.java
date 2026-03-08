package com.example.concertreservation.user.domain;

import com.example.concertreservation.pointHistory.domain.PointHistory;
import com.example.concertreservation.pointHistory.domain.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointCharger {

    private final PointHistoryRepository pointHistoryRepository;
    private final UserRepository userRepository;

    // 기존 코드
    public Long chargedPoint(User user, Long addedPoint) {
        user.chargedPoint(addedPoint);
        Long currentPoint = user.getPoint() + addedPoint;
        PointHistory newChargeHistory = PointHistory.chargeHistory(user, addedPoint, currentPoint);
        pointHistoryRepository.save(newChargeHistory);
        return user.getUsersId();
    }

    // synchronized 키워드를 활용한 동시성 제어
    public Long chargedPoint2(User user, Long addedPoint) {
        user.chargedPoint(addedPoint);
        Long currentPoint = user.getPoint() + addedPoint;
        PointHistory newChargeHistory = PointHistory.chargeHistory(user, addedPoint, currentPoint);
        pointHistoryRepository.save(newChargeHistory);
        return user.getUsersId();
    }

    public Long chargedPointWithOptimisticLock(User user, Long addedPoint) {
        user.chargedPoint(addedPoint);
        Long currentPoint = user.getPoint() + addedPoint;
        PointHistory newChargeHistory = PointHistory.chargeHistory(user, addedPoint, currentPoint);
        pointHistoryRepository.save(newChargeHistory);
        return user.getUsersId();
    }

    public Long chargedPointWithPessimisticLock(User user, Long addedPoint) {
        user.chargedPoint(addedPoint);
        Long currentPoint = user.getPoint() + addedPoint;
        PointHistory newChargeHistory = PointHistory.chargeHistory(user, addedPoint, currentPoint);
        pointHistoryRepository.save(newChargeHistory);
        return user.getUsersId();
    }
}
