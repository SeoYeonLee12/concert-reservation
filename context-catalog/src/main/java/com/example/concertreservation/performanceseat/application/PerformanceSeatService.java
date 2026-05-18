package com.example.concertreservation.performanceseat.application;

import com.example.concertreservation.performanceseat.application.result.PerformanceSeatResult;
import com.example.concertreservation.performanceseat.domain.PerformanceSeatRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PerformanceSeatService {

    private final PerformanceSeatRepository performanceSeatRepository;

    @Transactional(readOnly = true)
    public List<PerformanceSeatResult> findSeatsByScheduleId(Long scheduleId, Pageable pageable) {
        return performanceSeatRepository.findAllByScheduleScheduleId(scheduleId, pageable)
                .getContent()
                .stream()
                .map(PerformanceSeatResult::from)
                .toList();
    }
}
