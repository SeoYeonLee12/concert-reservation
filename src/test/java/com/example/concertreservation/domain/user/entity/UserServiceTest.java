package com.example.concertreservation.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.example.concertreservation.user.application.UserService;
import com.example.concertreservation.user.application.command.UserSignupCommand;
import com.example.concertreservation.user.domain.User;
import com.example.concertreservation.user.domain.UserRepository;
import com.example.concertreservation.user.domain.UserSignUp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSignUp userSignUp;

    @InjectMocks
    private UserService userService;

    /**
     * 비밀번호가 암호화되었는가?
     * <p>
     * 중복된 이메일이 들어오면 에러(409 Conflict)를 터뜨리는가?
     */

    /**
     * 1. Given (준비: 이런 상황이 주어졌을 때) 데이터 준비: 사용자가 입력했다고 가정할 JoinCommand 객체를 만듭니다. 여기에는 이메일, 이름, 그리고
     * **"rawPassword(평문 비밀번호)"**인 "1234"를 담습니다.
     * <p>
     * 저장소(Repository) 교육: 가짜 UserRepository에게도 알려줍니다. "이 이메일로 중복 체크(existsByEmail)를 하면,
     * **'false(없음)'**라고 대답해서 통과시켜 줘."
     * <p>
     * 2. When (실행: 이 행동을 하면) 테스트 대상인 UserService의 join() 메서드를 호출하면서, 위에서 만든 JoinCommand를 넘겨줍니다.
     * <p>
     * 3. Then (검증: 이런 결과가 나와야 한다) 핵심 검증 (Argument Captor): UserRepository의 save() 메서드가 호출될 때, 그
     * 메서드에 **매개변수로 넘겨진 유저 객체(User)**를 몰래 낚아챕니다(Capture). UserRepository.save()가 호출될 때 캡처한 User 객체의
     * 비밀번호를 확인합니다.
     * <p>
     * "AABBCC여야 해"라고 값을 강제하는 대신, "적어도 평문('1234')은 아니어야 한다" 혹은 "64글자(SHA-256 길이)여야 한다" 정도로만 검증합니다.
     */
    @Test
    public void 비밀번호가_암호화되어_저장된다() {

        // given
        UserSignupCommand command = new UserSignupCommand("test1@test.com", "password123", "이름1",
                "닉네임1");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // when
        userService.registerUser(command);

        // then
        verify(userSignUp).saveUser(userCaptor.capture());
        User newUser = userCaptor.getValue();
        assertThat(newUser.getPassword().getHashedPassword().length()).isEqualTo(64);
        System.out.println("암호화된 비밀번호: " + newUser.getPassword().getHashedPassword());
    }


}
