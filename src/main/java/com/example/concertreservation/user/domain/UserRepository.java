package com.example.concertreservation.user.domain;

import com.example.concertreservation.global.error.errorcode.UserErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Lock(value = LockModeType.OPTIMISTIC)
    @Query("SELECT u FROM User u WHERE u.usersId= :userId ")
    User findByWithOptimisticLock(Long userId);
    
    default User getUserById(Long id) {
        return findUserByUsersId(id).orElseThrow(
                () -> new GlobalException(UserErrorCode.USER_NOT_FOUND));
    }

    Optional<User> findUserByUsersId(Long id);

    boolean existsUserByEmail(String email);

    default User getByEmail(String email) {
        return findByEmail(email).orElseThrow(
                () -> new GlobalException(UserErrorCode.USER_NOT_FOUND));
    }

    Optional<User> findByEmail(String email);
}
