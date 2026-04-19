package com.example.concertreservation.performance.presentation;

import com.example.concertreservation.performance.application.PerformanceService;
import com.example.concertreservation.performance.application.result.PerformanceGetResult;
import com.example.concertreservation.performance.application.result.PerformanceListResult;
import com.example.concertreservation.performance.application.result.PerformanceScheduleListResult;
import com.example.concertreservation.performance.presentation.dto.PerformanceGetResponse;
import com.example.concertreservation.performance.presentation.dto.PerformanceListResponse;
import com.example.concertreservation.performance.presentation.dto.PerformanceScheduleListResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/performances")
public class PerformanceController {

    private final PerformanceService performanceService;

    @GetMapping
    public ResponseEntity<PerformanceListResponse> getPerformanceList() {
        List<PerformanceListResult> results = performanceService.findPerformanceList();
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
}
