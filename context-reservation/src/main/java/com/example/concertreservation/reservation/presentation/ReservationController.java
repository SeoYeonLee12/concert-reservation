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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 좌석 선점 API.
     * strategy: redisson(기본) | named-lock | optimistic
     */
    @PostMapping
    public ResponseEntity<ReservationCreateResponse> tryReserve(
            @Auth Long userId,
            @RequestBody @Valid ReservationCreateRequest request,
            @RequestParam(defaultValue = "redisson") String strategy
    ) {
        Long reservationId = reservationService.tryReserve(userId, request.performanceSeatId(), strategy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ReservationCreateResponse(reservationId));
    }

    @PostMapping("/{reservationId}/confirm")
    public ResponseEntity<Void> confirmPayment(
            @Auth Long userId,
            @PathVariable Long reservationId
    ) {
        reservationService.confirmPayment(userId, reservationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<Void> cancelReservation(
            @Auth Long userId,
            @PathVariable Long reservationId
    ) {
        reservationService.cancelReservation(userId, reservationId);
        return ResponseEntity.ok().build();
    }
}
