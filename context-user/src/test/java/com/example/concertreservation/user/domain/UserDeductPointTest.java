package com.example.concertreservation.user.domain;

import com.example.concertreservation.global.error.errorcode.UserErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserDeductPointTest {

    private User createUserWithPoint(long initialPoint) {
        User user = new User("test@example.com", "password123!", "테스트", "tester");
        if (initialPoint > 0) {
            user.chargedPoint(initialPoint);
        }
        return user;
    }

    @Test
    void deductPoint_정상차감() {
        User user = createUserWithPoint(1000L);
        user.deductPoint(300L);
        assertThat(user.getPoint()).isEqualTo(700L);
    }

    @Test
    void deductPoint_0원차감_정상처리() {
        User user = createUserWithPoint(1000L);
        user.deductPoint(0L);
        assertThat(user.getPoint()).isEqualTo(1000L);
    }

    @Test
    void deductPoint_음수금액_예외발생() {
        User user = createUserWithPoint(1000L);
        assertThatThrownBy(() -> user.deductPoint(-100L))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getCode())
                        .isEqualTo(UserErrorCode.INVALID_DEDUCT_AMOUNT));
    }

    @Test
    void deductPoint_잔액부족_예외발생() {
        User user = createUserWithPoint(100L);
        assertThatThrownBy(() -> user.deductPoint(200L))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> assertThat(((GlobalException) ex).getCode())
                        .isEqualTo(UserErrorCode.INSUFFICIENT_POINT));
    }
}
