package com.example.concertreservation.global.event;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DomainEventRepository extends JpaRepository<DomainEvent, Long> {

    List<DomainEvent> findByStatusAndCreatedAtBefore(EventStatus status, LocalDateTime threshold);

    List<DomainEvent> findByStatusInAndCreatedAtBefore(Collection<EventStatus> statuses, LocalDateTime threshold);
}
