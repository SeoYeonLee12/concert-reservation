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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class Password {

    public static final String HASHED_ALGORITHM = "sha-256";

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(12);

    @Column(name = "password", nullable = false, length = 100)
    private String hashedPassword;

    private Password(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public static Password hashPassword(String password) {
        return new Password(BCRYPT.encode(password));
    }

    public boolean match(String plainPassword) {
        if (isBcryptHash()) {
            return BCRYPT.matches(plainPassword, this.hashedPassword);
        }
        return this.hashedPassword.equals(sha256(plainPassword));
    }

    public boolean isLegacyHash() {
        return !isBcryptHash();
    }

    public Password upgradeToHashed(String plainPassword) {
        return new Password(BCRYPT.encode(plainPassword));
    }

    private boolean isBcryptHash() {
        return this.hashedPassword != null
                && (this.hashedPassword.startsWith("$2a$")
                || this.hashedPassword.startsWith("$2b$")
                || this.hashedPassword.startsWith("$2y$"));
    }

    private static String sha256(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASHED_ALGORITHM);
            byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexStr = new StringBuilder();
            for (byte b : bytes) {
                hexStr.append(String.format("%02x", b));
            }
            return hexStr.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new GlobalException(UserErrorCode.INVALID_PASSWORD_ALGORITHM);
        }
    }
}
