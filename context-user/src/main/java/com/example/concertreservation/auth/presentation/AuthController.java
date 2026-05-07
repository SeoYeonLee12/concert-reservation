package com.example.concertreservation.auth.presentation;

import com.example.concertreservation.auth.Auth;
import com.example.concertreservation.auth.Token;
import com.example.concertreservation.auth.application.AuthService;
import com.example.concertreservation.auth.presentation.dto.ReissuedTokenRequest;
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
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping
    public ResponseEntity<Token> reissuedToken(
            @Auth Long userId,
            @RequestBody @Valid ReissuedTokenRequest request
    ) {
        Token newToken = authService.reissueToken(userId, request.refreshToken());
        return ResponseEntity.status(HttpStatus.OK).body(newToken);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Auth Long userId) {
        authService.deleteToken(userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
