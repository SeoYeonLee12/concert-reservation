package com.example.concertreservation.pointHistory.domain;

import com.example.concertreservation.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    Page<PointHistory> findAllByUser(User user, Pageable pageable);
}
