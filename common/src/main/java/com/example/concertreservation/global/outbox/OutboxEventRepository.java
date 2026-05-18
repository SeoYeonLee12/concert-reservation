package com.example.concertreservation.global.outbox;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusAndCreatedAtBefore(OutboxStatus status, LocalDateTime threshold);
}
