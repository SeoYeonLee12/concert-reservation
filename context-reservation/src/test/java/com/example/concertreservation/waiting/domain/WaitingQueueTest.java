package com.example.concertreservation.waiting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.concertreservation.global.error.exception.GlobalException;
import com.example.concertreservation.waiting.domain.enums.WaitingQueueStatus;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class WaitingQueueTest {

    @Test
    void 초기_상태는_WAITING() {
        WaitingQueue wq = new WaitingQueue(1L, 100L, 1);
        assertThat(wq.getStatus()).isEqualTo(WaitingQueueStatus.WAITING);
        assertThat(wq.getActivatedAt()).isNull();
    }

    @Test
    void WAITING에서_ACTIVE_전환_성공() {
        WaitingQueue wq = new WaitingQueue(1L, 100L, 1);
        wq.activate();

        assertThat(wq.getStatus()).isEqualTo(WaitingQueueStatus.ACTIVE);
        assertThat(wq.getActivatedAt()).isNotNull();
    }

    @Test
    void ACTIVE에서_DONE_전환_성공() {
        WaitingQueue wq = new WaitingQueue(1L, 100L, 1);
        wq.activate();
        wq.done();

        assertThat(wq.getStatus()).isEqualTo(WaitingQueueStatus.DONE);
    }

    @Test
    void WAITING에서_EXPIRED_전환_성공() {
        WaitingQueue wq = new WaitingQueue(1L, 100L, 1);
        wq.expire();

        assertThat(wq.getStatus()).isEqualTo(WaitingQueueStatus.EXPIRED);
    }

    @Test
    void ACTIVE에서_EXPIRED_전환_성공() {
        WaitingQueue wq = new WaitingQueue(1L, 100L, 1);
        wq.activate();
        wq.expire();

        assertThat(wq.getStatus()).isEqualTo(WaitingQueueStatus.EXPIRED);
    }

    @Test
    void DONE_상태에서_activate_시도시_예외() {
        WaitingQueue wq = new WaitingQueue(1L, 100L, 1);
        wq.activate();
        wq.done();

        assertThatThrownBy(wq::activate).isInstanceOf(GlobalException.class);
    }

    @Test
    void WAITING_상태에서_done_시도시_예외() {
        WaitingQueue wq = new WaitingQueue(1L, 100L, 1);

        assertThatThrownBy(wq::done).isInstanceOf(GlobalException.class);
    }

    @Test
    void DONE_상태에서_expire_시도시_예외() {
        WaitingQueue wq = new WaitingQueue(1L, 100L, 1);
        wq.activate();
        wq.done();

        assertThatThrownBy(wq::expire).isInstanceOf(GlobalException.class);
    }

    @Test
    void ACTIVE_상태_5분_초과시_isExpired_true() {
        WaitingQueue wq = new WaitingQueue(1L, 100L, 1);
        wq.activate();

        LocalDateTime now = wq.getActivatedAt().plus(Duration.ofMinutes(6));
        assertThat(wq.isExpired(now, Duration.ofMinutes(5))).isTrue();
    }

    @Test
    void ACTIVE_상태_5분_미만이면_isExpired_false() {
        WaitingQueue wq = new WaitingQueue(1L, 100L, 1);
        wq.activate();

        LocalDateTime now = wq.getActivatedAt().plus(Duration.ofMinutes(3));
        assertThat(wq.isExpired(now, Duration.ofMinutes(5))).isFalse();
    }

    @Test
    void WAITING_상태에서_isExpired_항상_false() {
        WaitingQueue wq = new WaitingQueue(1L, 100L, 1);

        assertThat(wq.isExpired(LocalDateTime.now().plusHours(1), Duration.ofMinutes(5))).isFalse();
    }
}
