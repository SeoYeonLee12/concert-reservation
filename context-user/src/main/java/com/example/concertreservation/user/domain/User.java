package com.example.concertreservation.user.domain;

import com.example.concertreservation.global.domain.SoftDeletedDomain;
import com.example.concertreservation.global.error.errorcode.UserErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends SoftDeletedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "users_id")
    private Long usersId;

    @Column(name = "email", unique = true)
    private String email;

    @Embedded
    private Password password;

    @Column(name = "name")
    private String name;

    @Column(name = "nickname", unique = true)
    private String nickName;

    @Column(name = "point")
    private Long point;

    public User(String email, String password, String name, String nickName) {
        this.email = email;
        this.password = Password.hashPassword(password);
        this.name = name;
        this.nickName = nickName;
        this.point = 0L;
    }

    public void login(String plainTextPassword) {
        boolean same = this.password.match(plainTextPassword);
        if (!same) {
            throw new GlobalException(UserErrorCode.INVALID_USERNAME_PASSWORD);
        }
        // 점진 마이그레이션: legacy SHA-256이면 BCrypt로 재해시
        if (this.password.isLegacyHash()) {
            this.password = this.password.upgradeToHashed(plainTextPassword);
        }
    }

    public void chargedPoint(Long addedPoint) {
        if (addedPoint < 0) {
            throw new GlobalException(UserErrorCode.INVALID_CHARGE_AMOUNT);
        }
        this.point += addedPoint;
    }

    public void deductPoint(Long amount) {
        if (amount < 0) {
            throw new GlobalException(UserErrorCode.INVALID_DEDUCT_AMOUNT);
        }
        if (this.point < amount) {
            throw new GlobalException(UserErrorCode.INSUFFICIENT_POINT);
        }
        this.point -= amount;
    }
}
