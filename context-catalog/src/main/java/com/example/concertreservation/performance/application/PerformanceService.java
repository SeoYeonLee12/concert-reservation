package com.example.concertreservation.performance.application;

import com.example.concertreservation.global.cache.CacheStampedeGuard;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final ScheduleRepository scheduleRepository;
    private final CacheStampedeGuard cacheStampedeGuard;

    /**
     * [Step 0] 보호 없는 기본 @Cacheable — Cache Stampede Before 측정용.
     * 캐시 만료 시 모든 스레드가 동시에 DB를 조회해 Stampede 발생.
     */
    @Cacheable(cacheNames = "performanceList",
               key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public List<PerformanceListResult> findPerformanceListUnsafe(Pageable pageable) {
        Page<Performance> page = performanceRepository.findAllList(pageable);
        return page.getContent().stream()
                .map(PerformanceListResult::from)
                .toList();
    }

    /**
     * [Step 1] @Cacheable(sync=true) — JVM 로컬 동기화.
     * 같은 프로세스 내에서 캐시 미스 시 단 1개 스레드만 DB 조회.
     * 단일 인스턴스 환경에서 Cache Stampede 방어.
     */
    @Cacheable(cacheNames = "performanceList",
               key = "#pageable.pageNumber + '-' + #pageable.pageSize",
               sync = true)
    @Transactional(readOnly = true)
    public List<PerformanceListResult> findPerformanceList(Pageable pageable) {
        Page<Performance> page = performanceRepository.findAllList(pageable);
        return page.getContent().stream()
                .map(PerformanceListResult::from)
                .toList();
    }

    /**
     * [Step 2] Redisson 분산 락 — 멀티 인스턴스 분산 환경 보호.
     * 여러 서버가 동시에 같은 캐시 키를 갱신하려 할 때 단 1개 서버만 DB 조회.
     * @Cacheable(sync=true)의 분산 버전.
     */
    @Transactional(readOnly = true)
    public List<PerformanceListResult> findPerformanceListDistributed(Pageable pageable) {
        String key = pageable.getPageNumber() + "-" + pageable.getPageSize();
        return cacheStampedeGuard.protect("performanceList", key, () -> {
            Page<Performance> page = performanceRepository.findAllList(pageable);
            return page.getContent().stream()
                    .map(PerformanceListResult::from)
                    .toList();
        });
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
