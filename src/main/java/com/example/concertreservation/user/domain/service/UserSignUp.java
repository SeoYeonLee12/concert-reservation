package com.example.concertreservation.user.domain.service;

import com.example.concertreservation.global.error.errorcode.UserErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import com.example.concertreservation.user.domain.User;
import com.example.concertreservation.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSignUp {

  private final UserRepository userRepository;

  public User saveUser(User user) {
    if (userRepository.existsUserByEmail(user.getEmail())) {
      throw new GlobalException(UserErrorCode.DUPLICATED_EMAIL);
    }
    return userRepository.save(user);
  }
}
