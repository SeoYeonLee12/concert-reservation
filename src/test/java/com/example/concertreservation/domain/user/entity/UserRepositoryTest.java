package com.example.concertreservation.domain.user.entity;

import com.example.concertreservation.global.domain.BaseDomain;
import com.example.concertreservation.user.domain.User;
import com.example.concertreservation.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({BaseDomain.class})
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    /**
     * ① 회원 저장 및 조회 테스트 (기본 CRUD) 가장 기본적인 기능이 잘 동작하는지 확인합니다.
     * <p>
     * 준비 (Given): new User(...)를 통해 테스트용 유저 객체를 하나 생성합니다. (아직 ID는 없는 상태)
     * <p>
     * 실행 (When): userRepository.save() 메서드를 호출하여 유저를 저장합니다. 그리고 반환된 유저 객체의 ID를 확인하거나, findById()로
     * 다시 조회해 봅니다.
     * <p>
     * 검증 (Then):
     * <p>
     * 저장된 유저의 ID가 존재하는지(null이 아닌지) 확인합니다. (Auto Increment 동작 확인)
     * <p>
     * 처음 생성할 때 넣었던 이름, 이메일 등의 정보가 조회된 객체의 정보와 일치하는지 비교합니다.
     */
    @DisplayName("회원 저장 및 조회 테스트")
    @Test
    public void saveUser() {

        //given
        User testUser = new User(
                "test1@test.com",
                "test123",
                "테스터1",
                "닉네임테스트"
        );

        // when
        userRepository.save(testUser);
        System.out.println("테스터1 id" + testUser.getId());

        // then
//    assertTrue(userRepository.existsById(testUser.getId()), "해당 유저의 아이디가 존재함.");
        User actualUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertAll(
                () -> assertEquals(testUser.getEmail(), actualUser.getEmail()),
                () -> assertEquals(testUser.getPassword(), actualUser.getPassword()),
                () -> assertEquals(testUser.getName(), actualUser.getName()),
                () -> assertEquals(testUser.getNickName(), actualUser.getNickName()),
                () -> assertNotNull(actualUser.getCreatedAt())
        );
    }


}
