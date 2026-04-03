package com.example.concertreservation.performance.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PerformanceRepository extends JpaRepository<Performance, Long> {

    @Query("SELECT DISTINCT p FROM Performance p " +
            "JOIN FETCH p.schedules s " +
            "JOIN FETCH s.place")
    List<Performance> findAllList();

}
