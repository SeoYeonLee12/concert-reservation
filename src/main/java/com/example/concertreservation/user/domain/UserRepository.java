package com.example.concertreservation.user.domain;

import com.example.concertreservation.global.error.errorcode.UserErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsUserByEmail(String email);

    default User getByEmail(String email) {
        return findByEmail(email).orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));
    }

    Optional<User> findByEmail(String email);
}
