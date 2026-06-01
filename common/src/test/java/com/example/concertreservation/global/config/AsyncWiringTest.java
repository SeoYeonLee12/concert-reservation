package com.example.concertreservation.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @Async("EVENT_ASYNC_TASK_EXECUTOR") qualifier가 AsyncConfig 빈에 실제로 연결되는지 검증.
 * 이름 오타 시 컴파일 에러 없이 SimpleAsyncTaskExecutor로 폴백되므로, 빈 주입 자체를 슬라이스로 검증.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AsyncConfig.class)
class AsyncWiringTest {

    @Autowired
    @Qualifier("EVENT_ASYNC_TASK_EXECUTOR")
    private Executor executor;

    @Test
    void EVENT_ASYNC_TASK_EXECUTOR_빈이_ThreadPoolTaskExecutor로_등록된다() {
        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
    }

    @Test
    void 스레드풀_설정값이_AsyncConfig_명세와_일치한다() {
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) executor;
        assertThat(pool.getCorePoolSize()).isEqualTo(2);
        assertThat(pool.getMaxPoolSize()).isEqualTo(4);
        assertThat(pool.getThreadNamePrefix()).isEqualTo("event-async-");
    }
}
