package com.example.concertreservation.reservation.presentation;

import com.example.concertreservation.auth.Auth;
import com.example.concertreservation.reservation.application.ReservationService;
import com.example.concertreservation.reservation.presentation.dto.ReservationCreateRequest;
import com.example.concertreservation.reservation.presentation.dto.ReservationCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationCreateResponse> tryReserve(
            @Auth Long userId,
            @RequestBody @Valid ReservationCreateRequest request
    ) {
        Long reservationId = reservationService.tryReserve(userId, request.performanceSeatId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ReservationCreateResponse(reservationId));
    }
}
