package com.example.concertreservation.performance.presentation;

import com.example.concertreservation.performance.application.PerformanceService;
import com.example.concertreservation.performance.application.result.PerformanceGetResult;
import com.example.concertreservation.performance.application.result.PerformanceListResult;
import com.example.concertreservation.performance.application.result.PerformanceScheduleListResult;
import com.example.concertreservation.performance.presentation.dto.PerformanceGetResponse;
import com.example.concertreservation.performance.presentation.dto.PerformanceListResponse;
import com.example.concertreservation.performance.presentation.dto.PerformanceScheduleListResponse;
import com.example.concertreservation.performanceseat.application.PerformanceSeatService;
import com.example.concertreservation.performanceseat.application.result.PerformanceSeatResult;
import com.example.concertreservation.performanceseat.presentation.dto.PerformanceSeatListResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/performances")
public class PerformanceController {

    private final PerformanceService performanceService;
    private final PerformanceSeatService performanceSeatService;

    /**
     * ?strategy=distributed 파라미터로 Cache Stampede 방어 전략 전환 가능.
     * 기본값: sync=true (JVM 로컬 동기화)
     * distributed: Redisson 분산 락 (멀티 인스턴스 보호)
     * k6 부하 테스트로 두 전략의 p95/p99 차이를 측정.
     */
    @GetMapping
    public ResponseEntity<PerformanceListResponse> getPerformanceList(
            @PageableDefault(size = 20, sort = "performanceId", direction = Sort.Direction.DESC)
            Pageable pageable,
            @RequestParam(defaultValue = "sync") String strategy
    ) {
        List<PerformanceListResult> results = switch (strategy) {
            case "distributed" -> performanceService.findPerformanceListDistributed(pageable);
            case "none"        -> performanceService.findPerformanceListUnsafe(pageable);
            default            -> performanceService.findPerformanceList(pageable);
        };
        return ResponseEntity.status(HttpStatus.OK).body(PerformanceListResponse.from(results));
    }

    @GetMapping("{performanceId}")
    public ResponseEntity<PerformanceGetResponse> getPerformance(@PathVariable Long performanceId) {
        PerformanceGetResult result = performanceService.findPerformanceById(performanceId);
        return ResponseEntity.status(HttpStatus.OK).body(PerformanceGetResponse.from(result));
    }

    @GetMapping("/{performanceId}/schedules")
    public ResponseEntity<PerformanceScheduleListResponse> getPerformanceSchedules(
            @PathVariable Long performanceId
    ) {
        List<PerformanceScheduleListResult> result =
                performanceService.findPerformanceScheduleList(performanceId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(PerformanceScheduleListResponse.from(result));
    }

    @GetMapping("/{performanceId}/schedules/{scheduleId}/seats")
    public ResponseEntity<PerformanceSeatListResponse> getPerformanceSeats(
            @PathVariable Long performanceId,
            @PathVariable Long scheduleId,
            @PageableDefault(size = 200, sort = "performanceSeatId", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        List<PerformanceSeatResult> results = performanceSeatService.findSeatsByScheduleId(scheduleId, pageable);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(PerformanceSeatListResponse.from(results));
    }
}
