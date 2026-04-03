package com.example.concertreservation.performance.domain;

import com.example.concertreservation.global.error.errorcode.PerformanceErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PerformanceRepository extends JpaRepository<Performance, Long> {

    @Query("SELECT DISTINCT p FROM Performance p " +
            "JOIN FETCH p.schedules s " +
            "JOIN FETCH s.place")
    List<Performance> findAllList();

    default Performance getByPerformanceId(Long performanceId) {
        return findByPerformanceId(performanceId).orElseThrow(
                () -> new GlobalException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));
    }

    @Query("SELECT DISTINCT p FROM Performance p " +
            "JOIN FETCH p.schedules s " +
            "JOIN FETCH s.place " +
            "WHERE p.performanceId = :performanceId")
    Optional<Performance> findByPerformanceId(Long performanceId);
}
