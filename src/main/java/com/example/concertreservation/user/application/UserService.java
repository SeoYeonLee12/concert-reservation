package com.example.concertreservation.user.application;

import com.example.concertreservation.auth.Token;
import com.example.concertreservation.auth.TokenProperty;
import com.example.concertreservation.auth.TokenService;
import com.example.concertreservation.user.application.command.UserSignupCommand;
import com.example.concertreservation.user.application.result.UserInfoResult;
import com.example.concertreservation.user.application.result.UserLoginResult;
import com.example.concertreservation.user.domain.User;
import com.example.concertreservation.user.domain.UserRepository;
import com.example.concertreservation.user.domain.UserSignUp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserSignUp userSignUp;
    private final TokenService tokenService;
    private final RedisService redisService;
    private final TokenProperty tokenProperty;

    public Long registerUser(UserSignupCommand command) {
        User registeredUser = command.toUser();
        userSignUp.saveUser(registeredUser);
        return registeredUser.getUserId();
    }

    @Transactional
    public UserLoginResult login(String email, String password) {
        User user = userRepository.getByEmail(email);
        user.login(password);

        Token token = tokenService.issueTokens(user.getUserId());
        redisService.save(
                user.getUserId(),
                token.refreshToken(),
                tokenProperty.refreshTokenExpirationMillis()
        );

        return new UserLoginResult(
                user.getUserId(),
                token.accessToken(),
                token.refreshToken()
        );
    }

    public UserInfoResult getUser(Long userId) {
        User user = userRepository.getUserById(userId);
        return UserInfoResult.from(user);
    }
}
