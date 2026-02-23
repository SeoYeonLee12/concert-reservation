package com.example.concertreservation.user.application;

import com.example.concertreservation.auth.Token;
import com.example.concertreservation.auth.TokenProperty;
import com.example.concertreservation.auth.TokenService;
import com.example.concertreservation.global.aop.ExecutionTime;
import com.example.concertreservation.user.application.command.UserSignupCommand;
import com.example.concertreservation.user.application.result.UserInfoResult;
import com.example.concertreservation.user.application.result.UserLoginResult;
import com.example.concertreservation.user.domain.PointCharge;
import com.example.concertreservation.user.domain.User;
import com.example.concertreservation.user.domain.UserRepository;
import com.example.concertreservation.user.domain.UserSignUp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserSignUp userSignUp;
    private final TokenService tokenService;
    private final RedisService redisService;
    private final TokenProperty tokenProperty;
    private final PointCharge pointCharge;

    // TODO 여기 개선
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

    // 기존 메서드
    @ExecutionTime
    @Transactional
    public Long chargePoint(Long userId, Long addedPoint) {
        User user = userRepository.getUserById(userId);
        return pointCharge.chargedPoint(user, addedPoint);
    }

    // Transaction+Synchronized로 Lock 적용
    @ExecutionTime
    @Transactional
    public synchronized Long chargePointSynchronizedFail(Long userId, Long addedPoint) {

        String threadName = Thread.currentThread().getName();
        log.info("[{}] Lock 획득", threadName);

        User user = userRepository.getUserById(userId);
        String point = user.getPoint().toString();
        log.info("current my point:{} ", point);

        Long result = pointCharge.chargedPoint2(user, addedPoint);
        log.info("current my point:{} ", point);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void beforeCommit(boolean readOnly) {
                        log.warn("[{}] 커밋 직전이지만 Lock은 해제됨.", threadName);
                    }

                    @Override
                    public void afterCommit() {
                        log.info("[{}] DB 커밋 완료", threadName);
                    }
                });

        log.info("[{}] Lock 해제 !! 메서드 종료", threadName);
        return result;
    }
}
