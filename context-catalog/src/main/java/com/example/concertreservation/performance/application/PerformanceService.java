package com.example.concertreservation.performance.application;

import com.example.concertreservation.performance.application.result.PerformanceGetResult;
import com.example.concertreservation.performance.application.result.PerformanceListResult;
import com.example.concertreservation.performance.application.result.PerformanceScheduleListResult;
import com.example.concertreservation.performance.domain.Performance;
import com.example.concertreservation.performance.domain.PerformanceRepository;
import com.example.concertreservation.performance.domain.Schedule;
import com.example.concertreservation.performance.domain.ScheduleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final ScheduleRepository scheduleRepository;

    @Cacheable(cacheNames = "performanceList")
    @Transactional(readOnly = true)
    public List<PerformanceListResult> findPerformanceList() {
        List<Performance> performances = performanceRepository.findAllList();
        return performances.stream()
                .map(PerformanceListResult::from)
                .toList();
    }

    @Cacheable(cacheNames = "performanceDetail", key = "#performanceId")
    @Transactional(readOnly = true)
    public PerformanceGetResult findPerformanceById(Long performanceId) {
        Performance performance = performanceRepository.getByPerformanceId(performanceId);
        return PerformanceGetResult.from(performance);
    }

    @Cacheable(cacheNames = "performanceSchedules", key = "#performanceId")
    @Transactional(readOnly = true)
    public List<PerformanceScheduleListResult> findPerformanceScheduleList(Long performanceId) {
        List<Schedule> schedules = scheduleRepository.getAllByPerformanceId(performanceId);
        return schedules.stream()
                .map(PerformanceScheduleListResult::from)
                .toList();
    }
}
