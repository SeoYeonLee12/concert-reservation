package com.example.concertreservation.user.presentation.dto;

public record UserSignUpResponse(
    Long userId
) {

  public static UserSignUpResponse from(Long userId) {
    return new UserSignUpResponse(userId);
  }
}
