package com.example.concertreservation.user.application;

import com.example.concertreservation.auth.Token;
import com.example.concertreservation.auth.TokenProperty;
import com.example.concertreservation.auth.TokenService;
import com.example.concertreservation.pointHistory.domain.PointHistory;
import com.example.concertreservation.pointHistory.domain.PointHistoryRepository;
import com.example.concertreservation.user.application.command.UserSignupCommand;
import com.example.concertreservation.user.application.result.PointHistoryResult;
import com.example.concertreservation.user.application.result.UserInfoResult;
import com.example.concertreservation.user.application.result.UserLoginResult;
import com.example.concertreservation.user.domain.PointCharger;
import com.example.concertreservation.user.domain.User;
import com.example.concertreservation.user.domain.UserRepository;
import com.example.concertreservation.user.domain.UserSignUp;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserSignUp userSignUp;
    private final TokenService tokenService;
    private final RedisService redisService;
    private final TokenProperty tokenProperty;
    private final PointCharger pointCharger;
    private final PointHistoryRepository pointHistoryRepository;

    public Long registerUser(UserSignupCommand command) {
        User registeredUser = command.toUser();
        userSignUp.saveUser(registeredUser);
        return registeredUser.getUsersId();
    }

    @Transactional
    public UserLoginResult login(String email, String password) {
        User user = userRepository.getByEmail(email);
        user.login(password);

        Token token = tokenService.issueTokens(user.getUsersId());
        redisService.save(
                user.getUsersId(),
                token.refreshToken(),
                tokenProperty.refreshTokenExpirationMillis()
        );

        return new UserLoginResult(
                user.getUsersId(),
                token.accessToken(),
                token.refreshToken()
        );
    }

    public UserInfoResult getUser(Long userId) {
        User user = userRepository.getUserById(userId);
        return UserInfoResult.from(user);
    }

    @Transactional
    public Long chargePoint(Long userId, Long addedPoint) {
        User user = userRepository.findByUsersIdForUpdate(userId);
        return pointCharger.chargePoint(user, addedPoint);
    }

    public Long findCurrentPoint(Long userId) {
        return userRepository.findPointByUsersId(userId);
    }

    @Transactional(readOnly = true)
    public List<PointHistoryResult> findPointHistories(Long userId) {
        User user = userRepository.getUserById(userId);
        List<PointHistory> pointHistoryList = pointHistoryRepository.findAllByUser(user);
        return pointHistoryList.stream()
                .map(PointHistoryResult::from)
                .toList();
    }
}
