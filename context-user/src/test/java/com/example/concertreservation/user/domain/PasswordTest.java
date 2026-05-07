package com.example.concertreservation.user.domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordTest {

    @Test
    void hashPassword_같은_평문_두번_호출시_다른_해시_반환() {
        Password p1 = Password.hashPassword("plaintext");
        Password p2 = Password.hashPassword("plaintext");
        assertThat(p1.getHashedPassword()).isNotEqualTo(p2.getHashedPassword());
    }

    @Test
    void match_올바른_평문_입력시_true() {
        Password password = Password.hashPassword("correct!");
        assertThat(password.match("correct!")).isTrue();
    }

    @Test
    void match_잘못된_평문_입력시_false() {
        Password password = Password.hashPassword("correct!");
        assertThat(password.match("wrong!")).isFalse();
    }

    @Test
    void isLegacyHash_BCrypt_해시면_false() {
        Password password = Password.hashPassword("test");
        assertThat(password.isLegacyHash()).isFalse();
    }

    @Test
    void isLegacyHash_SHA256_해시면_true() throws Exception {
        Password password = injectLegacyHash("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3");
        assertThat(password.isLegacyHash()).isTrue();
    }

    @Test
    void upgradeToHashed_레거시에서_BCrypt로_변환() throws Exception {
        // SHA-256 hash of "test" = 9f86d081...
        Password legacy = injectLegacyHash("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
        assertThat(legacy.isLegacyHash()).isTrue();
        assertThat(legacy.match("test")).isTrue();

        Password upgraded = legacy.upgradeToHashed("test");
        assertThat(upgraded.getHashedPassword()).startsWith("$2");
        assertThat(upgraded.match("test")).isTrue();
    }

    private Password injectLegacyHash(String sha256hex) throws Exception {
        Password password = Password.hashPassword("dummy");
        Field field = Password.class.getDeclaredField("hashedPassword");
        field.setAccessible(true);
        field.set(password, sha256hex);
        return password;
    }
}
