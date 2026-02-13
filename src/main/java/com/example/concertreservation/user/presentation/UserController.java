package com.example.concertreservation.user.presentation;

import com.example.concertreservation.auth.Auth;
import com.example.concertreservation.auth.TokenService;
import com.example.concertreservation.user.application.UserService;
import com.example.concertreservation.user.application.command.UserSignupCommand;
import com.example.concertreservation.user.application.result.UserInfoResult;
import com.example.concertreservation.user.application.result.UserLoginResult;
import com.example.concertreservation.user.presentation.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TokenService tokenService;

    @PostMapping("/signup")
    public ResponseEntity<UserSignUpResponse> userSignUp(
            @RequestBody @Valid UserSignUpRequest request) {
        UserSignupCommand command = request.toCommand();
        Long signupUserId = userService.registerUser(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(new UserSignUpResponse(signupUserId));
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> login(@RequestBody LoginRequest request) {
        UserLoginResult result = userService.login(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.OK).body(UserLoginResponse.from(result));
    }

    @GetMapping
    public ResponseEntity<UserInfoResponse> getUser(@Auth Long userId) {
        UserInfoResult result = userService.getUser(userId);
        return ResponseEntity.status(HttpStatus.OK).body(UserInfoResponse.from(result));
    }
}
