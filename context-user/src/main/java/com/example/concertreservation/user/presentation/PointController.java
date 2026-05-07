package com.example.concertreservation.user.presentation;

import com.example.concertreservation.auth.Auth;
import com.example.concertreservation.user.application.UserService;
import com.example.concertreservation.user.application.result.PointHistoryResult;
import com.example.concertreservation.user.presentation.dto.PointChargeRequest;
import com.example.concertreservation.user.presentation.dto.PointChargeResponse;
import com.example.concertreservation.user.presentation.dto.PointHistoryListResponse;
import com.example.concertreservation.user.presentation.dto.UserPointResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<PointChargeResponse> chargePoint(
            @Auth Long userId,
            @RequestBody PointChargeRequest request
    ) {
        Long updatedPoint = userService.chargePoint(userId, request.updatePointAmount());
        return ResponseEntity.status(HttpStatus.OK).body(new PointChargeResponse(updatedPoint));
    }

    @GetMapping
    public ResponseEntity<UserPointResponse> getUserPoint(@Auth Long userId) {
        Long userPoint = userService.findCurrentPoint(userId);
        return ResponseEntity.status(HttpStatus.OK).body(new UserPointResponse(userPoint));
    }

    @GetMapping("/history")
    ResponseEntity<PointHistoryListResponse> getPointHistory(@Auth Long userId) {
        List<PointHistoryResult> results = userService.findPointHistories(userId);
        PointHistoryListResponse response = PointHistoryListResponse.from(results);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
