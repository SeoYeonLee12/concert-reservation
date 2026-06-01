package com.example.concertreservation.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * SimpleAsyncTaskExecutor(풀 없음) vs ThreadPoolTaskExecutor(EVENT_ASYNC_TASK_EXECUTOR) 스레드 동작 비교.
 * Spring 컨텍스트 없음 — Executor 단위 동작만 검증. @EnableAsync 와이어링은 별도 슬라이스 테스트로 보강 가능.
 */
class AsyncPoolConfigTest {

    private static final int TASK_COUNT = 10;

    @Test
    void 풀_executor는_스레드를_재사용한다() throws InterruptedException {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new AsyncConfig().eventAsyncTaskExecutor();
        Set<String> threadNames = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);

        for (int i = 0; i < TASK_COUNT; i++) {
            executor.execute(() -> {
                threadNames.add(Thread.currentThread().getName());
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);

        assertThat(threadNames).hasSizeLessThanOrEqualTo(4);
        assertThat(threadNames).allMatch(name -> name.startsWith("event-async-"));

        executor.shutdown();
    }

    @Test
    void 풀_executor_스레드명은_event_async_패턴을_따른다() throws InterruptedException {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new AsyncConfig().eventAsyncTaskExecutor();
        CountDownLatch latch = new CountDownLatch(1);
        String[] capturedName = new String[1];

        executor.execute(() -> {
            capturedName[0] = Thread.currentThread().getName();
            latch.countDown();
        });

        latch.await(5, TimeUnit.SECONDS);

        assertThat(capturedName[0]).matches("event-async-\\d+");

        executor.shutdown();
    }

    @Test
    void simple_executor는_호출_스레드와_다른_스레드에서_실행된다() throws InterruptedException {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("simple-test-");
        String callingThread = Thread.currentThread().getName();
        Set<String> executedThreadNames = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);

        for (int i = 0; i < TASK_COUNT; i++) {
            executor.execute(() -> {
                executedThreadNames.add(Thread.currentThread().getName());
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);

        // 모든 작업이 호출 스레드와 다른 스레드에서 실행됨 (비동기 확인)
        assertThat(executedThreadNames).doesNotContain(callingThread);
        // 풀 설정 없이 생성한 executor이므로 event-async- prefix 없음
        assertThat(executedThreadNames).noneMatch(name -> name.startsWith("event-async-"));
    }
}
