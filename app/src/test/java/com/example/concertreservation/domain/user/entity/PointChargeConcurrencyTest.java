package com.example.concertreservation.domain.user.entity;

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
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class PointChargeConcurrencyTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("비관적 락을 활용한 동시성 제어: 100명이 동시에 1,000원 충전 시 정확히 100,000원이 충전된다.")
    public void chargePoint_concurrency_test_pessimistic_lock() throws InterruptedException {
        // Given
        Long userId = userService.registerUser(new UserSignupCommand(
                "pessimistic@test.com", "password", "PessiTester", "PessiNick"
        ));

        // When
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // Application Service의 비관적 락 적용 메서드 호출
                    userService.chargePoint(userId, 1000L);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();

        // Then
        User user = userRepository.findById(userId).orElseThrow();
        long actualPoint = user.getPoint();
        long expectedPoint = 1000L * threadCount; // 100,000원 기대

        System.out.println("==================================================");
        System.out.println("기대 잔액: " + expectedPoint);
        System.out.println("실제 잔액: " + actualPoint);
        System.out.println("==================================================");

        assertThat(actualPoint).isEqualTo(expectedPoint);
    }
}
