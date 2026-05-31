package com.example.concertreservation.global.event;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeadLetterRepository extends JpaRepository<DeadLetter, Long> {

    List<DeadLetter> findByRecoveredFalse();
}
