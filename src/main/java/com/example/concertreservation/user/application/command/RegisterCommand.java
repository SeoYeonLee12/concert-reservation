package com.example.concertreservation.user.application.command;

import com.example.concertreservation.user.domain.User;

public record RegisterCommand(
    String email,
    String password,
    String name,
    String nickName
) {

  public User toUser() {
    return new User(email, password, name, nickName);
  }
}
