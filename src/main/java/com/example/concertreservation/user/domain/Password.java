package com.example.concertreservation.user.domain;

import com.example.concertreservation.global.error.errorcode.UserErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class Password {

  public static final String HASHED_ALGORITHM = "sha-256";

  @Column(name = "password", nullable = false)
  private String hashedPassword;

  private Password(String hashedPassword) {
    this.hashedPassword = hashedPassword;
  }

  public static Password hashPassword(String password) {
    return new Password(hash(password));
  }

  private static String hash(String password) {
    try {
      MessageDigest md = MessageDigest.getInstance(HASHED_ALGORITHM);
      // 바이트 배열에 저장
      byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));

      StringBuilder hexStr = new StringBuilder();
      // byte to 16
      for (byte b : bytes) {
        hexStr.append(String.format("%02x", b));
      }
      return hexStr.toString();

    } catch (NoSuchAlgorithmException e) {
      throw new GlobalException(UserErrorCode.INVALID_PASSWORD_ALGORITHM);
    }
  }
}
