package com.example.concertreservation.user.application;

import com.example.concertreservation.auth.Token;
import com.example.concertreservation.auth.TokenProperty;
import com.example.concertreservation.auth.TokenService;
import com.example.concertreservation.global.aop.ExecutionTime;
import com.example.concertreservation.global.aop.retry.Retry;
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

    // 기존 메서드
    @ExecutionTime
    @Transactional
    public Long chargePoint(Long userId, Long addedPoint) {
        User user = userRepository.getUserById(userId);
        return pointCharger.chargedPoint(user, addedPoint);
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

        Long result = pointCharger.chargedPoint2(user, addedPoint);
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

    @Retry
    @ExecutionTime
    @Transactional
    public Long chargedPointWithOptimisticLock(Long userId, Long addedPoint) {

        String threadName = Thread.currentThread().getName();
        log.info("[{}] 트랜잭션 진입", threadName);

        User user = userRepository.findByWithOptimisticLock(userId);
        log.info("[{}] 조회 완료 !! 현재 잔액: {}, 읽은 버전: {}",
                threadName,
                user.getPoint(),
                user.getVersion()
        );
        Long result = pointCharger.chargedPointWithOptimisticLock(user, addedPoint);

        log.info("[{}] 커밋 대기 중(메모리 연산은 완료됨)", threadName);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        log.info("[{}] DB 커밋 완료", threadName);
                    }
                }
        );
        return result;
    }

    // TODO 개선 필요
    @Transactional
    public Long chargedPointWithPessimisticLock(Long userId, Long addedPoint) {
        User user = userRepository.findByUsersIdForUpdate(userId);
        Long result = pointCharger.chargedPointWithPessimisticLock(user, addedPoint);
        return result;
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
