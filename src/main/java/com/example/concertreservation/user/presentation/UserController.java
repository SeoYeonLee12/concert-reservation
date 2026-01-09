package com.example.concertreservation.user.presentation;

import com.example.concertreservation.user.application.UserService;
import com.example.concertreservation.user.application.command.RegisterCommand;
import com.example.concertreservation.user.presentation.dto.UserSignUpRequest;
import com.example.concertreservation.user.presentation.dto.UserSignUpResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @PostMapping("/signup")
  public ResponseEntity<UserSignUpResponse> userSignUp(
      @RequestBody @Valid UserSignUpRequest request) {
    RegisterCommand command = request.toCommand();
    Long signupUserId = userService.registerUser(command);
    return ResponseEntity.status(HttpStatus.CREATED).body(new UserSignUpResponse(signupUserId));

  }
}
