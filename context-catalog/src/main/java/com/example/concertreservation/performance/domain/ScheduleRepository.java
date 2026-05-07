package com.example.concertreservation.performance.domain;

import com.example.concertreservation.global.error.errorcode.ScheduleErrorCode;
import com.example.concertreservation.global.error.exception.GlobalException;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    default List<Schedule> getAllByPerformanceId(Long performanceId) {
        List<Schedule> schedules = findAllByPerformancePerformanceId(performanceId);

        if (schedules.isEmpty()) {
            throw new GlobalException(ScheduleErrorCode.SCHEDULE_NOT_FOUND);
        }

        return schedules;
    }

    List<Schedule> findAllByPerformancePerformanceId(Long performanceId);

}
