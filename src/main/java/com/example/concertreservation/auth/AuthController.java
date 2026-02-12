package com.example.concertreservation.auth;

import com.example.concertreservation.auth.controller.dto.ReissuedTokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public ReissuedTokenResponse reissuedToken(@Auth Long userId, @RequestBody @Valid ReissuedTokenRequest request) {

    }

}
