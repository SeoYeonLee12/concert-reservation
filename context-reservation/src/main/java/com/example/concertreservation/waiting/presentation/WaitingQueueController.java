package com.example.concertreservation.waiting.presentation;

import com.example.concertreservation.auth.Auth;
import com.example.concertreservation.waiting.application.WaitingQueueService;
import com.example.concertreservation.waiting.presentation.dto.WaitingQueueEnterRequest;
import com.example.concertreservation.waiting.presentation.dto.WaitingQueueEnterResponse;
import com.example.concertreservation.waiting.presentation.dto.WaitingQueueStatusResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/waiting-queue")
public class WaitingQueueController {

    private final WaitingQueueService waitingQueueService;

    @PostMapping
    public ResponseEntity<WaitingQueueEnterResponse> enter(
            @Auth Long userId,
            @RequestBody @Valid WaitingQueueEnterRequest request
    ) {
        WaitingQueueEnterResponse response = waitingQueueService.enter(userId, request.performanceSeatId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{waitingQueueId}/status")
    public ResponseEntity<WaitingQueueStatusResponse> getStatus(
            @PathVariable Long waitingQueueId
    ) {
        return ResponseEntity.ok(waitingQueueService.getStatus(waitingQueueId));
    }
}
