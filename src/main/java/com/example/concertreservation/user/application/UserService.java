package com.example.concertreservation.user.application;

import com.example.concertreservation.user.application.command.RegisterCommand;
import com.example.concertreservation.user.domain.User;
import com.example.concertreservation.user.domain.UserRepository;
import com.example.concertreservation.user.domain.service.UserSignUp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final UserSignUp userSignUp;

  public Long registerUser(RegisterCommand command) {
    User registeredUser = command.toUser();
    userSignUp.saveUser(registeredUser);
    return registeredUser.getId();
  }

}
