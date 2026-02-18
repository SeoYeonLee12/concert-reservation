package com.example.concertreservation;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.concertreservation.user.application.UserService;
import com.example.concertreservation.user.application.command.UserSignupCommand;
import com.example.concertreservation.user.domain.User;
import com.example.concertreservation.user.domain.UserRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PointChargeConcurrencyTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("동시성 이슈 발생 : 100명이 동시에 1000원 충전 -> 잔액이 100000원이 될까?")
    public void chargePoint_concurrency_test() throws InterruptedException {
        // Given
        Long userId = userService.registerUser(new UserSignupCommand(
                "test@test.com", "password", "Tester", "TestNick"
        ));

        // when
        int threadCount = 100;
        // 비동기 작업 실행할 스레드풀
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // 작업이 끝날 때까지 메인 스레드 멈춤
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    userService.chargePoint(userId, 1000L);
                } finally {
                    // 카운트 감소
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();

        //Then
        User user = userRepository.getUserById(userId);
        long actualPoint = user.getPoint();
        long expectedPoint = 1000L * threadCount; // 100000원이어야 함

        System.out.println("==================================================");
        System.out.println("기대 잔액: " + expectedPoint);
        System.out.println("실제 잔액: " + actualPoint);
        System.out.println("==================================================");

        assertThat(actualPoint).isNotEqualTo(expectedPoint);
    }
}
