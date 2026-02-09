package com.example.concertreservation.user.application;

import com.example.concertreservation.user.application.command.UserSignupCommand;
import com.example.concertreservation.user.domain.User;
import com.example.concertreservation.user.domain.UserRepository;
import com.example.concertreservation.user.domain.UserSignUp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserSignUp userSignUp;

    public Long registerUser(UserSignupCommand command) {
        User registeredUser = command.toUser();
        userSignUp.saveUser(registeredUser);
        return registeredUser.getId();

    }

    public Long login(String username, String password) {
        User user = userRepository.getByUserName(username);
        user.login(password);
        return user.getId();
    }

}
