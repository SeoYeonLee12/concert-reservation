package com.example.concertreservation.performance.presentation;

import com.example.concertreservation.performance.application.PerformanceService;
import com.example.concertreservation.performance.application.result.PerformanceListResult;
import com.example.concertreservation.performance.presentation.dto.PerformanceListResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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
}
