package com.example.concertreservation.user.presentation.dto;

import com.example.concertreservation.user.application.command.UserSignupCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.UniqueElements;

public record UserSignUpRequest(

        @NotBlank
        @Email(message = "올바른 이메일 형식으로 입력해주세요.")
        String email,

        @NotBlank
        @Length(min = 8, message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.")
        String password,

        @NotBlank(message = "이름을 입력하세요.")
        String name,

        @NotBlank(message = "닉네임을 입력하세요.")
        String nickName
) {

    public UserSignupCommand toCommand() {
        return new UserSignupCommand(this.email, this.password, this.name, this.nickName);
    }
}
