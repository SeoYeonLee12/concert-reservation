package com.example.concertreservation.user.domain;

import com.example.concertreservation.global.error.errorcode.UserErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    default User getUserById(Long id) {
        return findUserByUserId(id).orElseThrow(
                () -> new GlobalException(UserErrorCode.USER_NOT_FOUND));
    }

    Optional<User> findUserByUserId(Long id);

    boolean existsUserByEmail(String email);

    default User getByEmail(String email) {
        return findByEmail(email).orElseThrow(
                () -> new GlobalException(UserErrorCode.USER_NOT_FOUND));
    }

    Optional<User> findByEmail(String email);
}
