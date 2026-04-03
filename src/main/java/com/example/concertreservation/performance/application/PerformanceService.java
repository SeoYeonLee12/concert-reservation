package com.example.concertreservation.performance.application;

import com.example.concertreservation.performance.application.result.PerformanceGetResult;
import com.example.concertreservation.performance.application.result.PerformanceListResult;
import com.example.concertreservation.performance.domain.Performance;
import com.example.concertreservation.performance.domain.PerformanceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PerformanceRepository performanceRepository;

    @Transactional(readOnly = true)
    public List<PerformanceListResult> findPerformanceList() {
        List<Performance> performances = performanceRepository.findAllList();
        return performances.stream()
                .map(PerformanceListResult::from)
                .toList();
    }

    public PerformanceGetResult findPerformanceById(Long performanceId) {
        Performance performance = performanceRepository.getByPerformanceId(performanceId);
        return PerformanceGetResult.from(performance);
    }
}
