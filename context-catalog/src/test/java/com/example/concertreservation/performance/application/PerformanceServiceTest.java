package com.example.concertreservation.performance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.concertreservation.performance.application.result.PerformanceGetResult;
import com.example.concertreservation.performance.application.result.PerformanceListResult;
import com.example.concertreservation.performance.application.result.PerformanceScheduleListResult;
import com.example.concertreservation.performance.domain.Performance;
import com.example.concertreservation.performance.domain.PerformanceRepository;
import com.example.concertreservation.performance.domain.Schedule;
import com.example.concertreservation.performance.domain.ScheduleRepository;
import com.example.concertreservation.place.domain.Place;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PerformanceServiceTest {

    @Mock
    private PerformanceRepository performanceRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @InjectMocks
    private PerformanceService performanceService;

    // -------------------------------------------------------------------------
    // findPerformanceList
    // -------------------------------------------------------------------------

    private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, 20);

    @Test
    void 공연_목록_조회시_저장소가_빈_페이지를_반환하면_빈_결과를_반환한다() {
        // given
        when(performanceRepository.findAllList(DEFAULT_PAGEABLE))
                .thenReturn(new PageImpl<>(List.of()));

        // when
        List<PerformanceListResult> result = performanceService.findPerformanceList(DEFAULT_PAGEABLE);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void 공연_목록_조회시_N건이_존재하면_N건의_결과를_반환한다() {
        // given
        Performance performance1 = createPerformance("공연1", "설명1", "poster1.jpg", 120, "15세", "가수1");
        Performance performance2 = createPerformance("공연2", "설명2", "poster2.jpg", 90, "전체", "가수2");
        when(performanceRepository.findAllList(DEFAULT_PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(performance1, performance2)));

        // when
        List<PerformanceListResult> result = performanceService.findPerformanceList(DEFAULT_PAGEABLE);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).performanceTitle()).isEqualTo("공연1");
        assertThat(result.get(1).performanceTitle()).isEqualTo("공연2");
    }

    @Test
    void 공연_목록_조회시_결과에_첫번째_스케줄의_장소명과_시작시간이_포함된다() {
        // given
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 19, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 1, 21, 0);
        Performance performance = createPerformanceWithSchedule("콘서트", "poster.jpg", "올림픽홀", start, end);
        when(performanceRepository.findAllList(DEFAULT_PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(performance)));

        // when
        List<PerformanceListResult> result = performanceService.findPerformanceList(DEFAULT_PAGEABLE);

        // then
        PerformanceListResult item = result.get(0);
        assertThat(item.placeName()).isEqualTo("올림픽홀");
        assertThat(item.scheduleStartTime()).isEqualTo(start);
        assertThat(item.scheduleEndTime()).isEqualTo(end);
        assertThat(item.performanceStatus()).isEqualTo("READY");
    }

    // -------------------------------------------------------------------------
    // findPerformanceById
    // -------------------------------------------------------------------------

    @Test
    void 공연_단건_조회시_모든_필드가_정확히_매핑된다() {
        // given
        Long performanceId = 1L;
        LocalDateTime start = LocalDateTime.of(2026, 7, 10, 18, 0);
        LocalDateTime end = LocalDateTime.of(2026, 7, 10, 20, 0);
        Performance performance = createPerformanceWithSchedule(
                "록 페스티벌", "rock.jpg", "잠실체육관", start, end);
        setField(performance, "performanceId", performanceId);
        setField(performance, "performanceDescription", "최고의 록 공연");
        setField(performance, "ageRating", "12세");
        setField(performance, "performer", "밴드X");
        when(performanceRepository.getByPerformanceId(performanceId)).thenReturn(performance);

        // when
        PerformanceGetResult result = performanceService.findPerformanceById(performanceId);

        // then
        assertThat(result.performanceId()).isEqualTo(performanceId);
        assertThat(result.performanceTitle()).isEqualTo("록 페스티벌");
        assertThat(result.performanceDescription()).isEqualTo("최고의 록 공연");
        assertThat(result.posterImage()).isEqualTo("rock.jpg");
        assertThat(result.placeName()).isEqualTo("잠실체육관");
        assertThat(result.scheduleStartTime()).isEqualTo(start);
        assertThat(result.scheduleEndTime()).isEqualTo(end);
        assertThat(result.performanceStatus()).isEqualTo("READY");
        assertThat(result.performer()).isEqualTo("밴드X");
        assertThat(result.ageRating()).isEqualTo("12세");
    }

    // -------------------------------------------------------------------------
    // findPerformanceScheduleList
    // -------------------------------------------------------------------------

    @Test
    void 공연_스케줄_조회시_N개의_스케줄이_모두_변환되어_반환된다() {
        // given
        Long performanceId = 2L;
        Place place = new Place("세종문화회관", "서울시 종로구", 1000);
        Performance performance = createPerformance("뮤지컬", "줄거리", "musical.jpg", 150, "전체", "배우A");
        LocalDateTime start1 = LocalDateTime.of(2026, 8, 1, 14, 0);
        LocalDateTime end1 = LocalDateTime.of(2026, 8, 1, 16, 30);
        LocalDateTime start2 = LocalDateTime.of(2026, 8, 2, 19, 0);
        LocalDateTime end2 = LocalDateTime.of(2026, 8, 2, 21, 30);
        Schedule schedule1 = new Schedule(performance, place, start1,
                start1.minusDays(7), 500, 500, end1);
        Schedule schedule2 = new Schedule(performance, place, start2,
                start2.minusDays(7), 500, 480, end2);
        setField(schedule1, "scheduleId", 10L);
        setField(schedule2, "scheduleId", 11L);
        when(scheduleRepository.getAllByPerformanceId(performanceId))
                .thenReturn(List.of(schedule1, schedule2));

        // when
        List<PerformanceScheduleListResult> result =
                performanceService.findPerformanceScheduleList(performanceId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).scheduleId()).isEqualTo(10L);
        assertThat(result.get(0).startTime()).isEqualTo(start1);
        assertThat(result.get(0).endTime()).isEqualTo(end1);
        assertThat(result.get(0).totalSeats()).isEqualTo(500);
        assertThat(result.get(0).availableSeats()).isEqualTo(500);
        assertThat(result.get(1).scheduleId()).isEqualTo(11L);
        assertThat(result.get(1).availableSeats()).isEqualTo(480);
    }

    // -------------------------------------------------------------------------
    // @Cacheable 어노테이션 검증
    // -------------------------------------------------------------------------

    @Test
    void findPerformanceList_메서드에_cacheNames_performanceList로_Cacheable이_선언되어_있다()
            throws NoSuchMethodException {
        Method method = PerformanceService.class.getMethod("findPerformanceList", Pageable.class);
        Cacheable cacheable = method.getAnnotation(Cacheable.class);

        assertThat(cacheable).isNotNull();
        assertThat(cacheable.cacheNames()).containsExactly("performanceList");
        assertThat(cacheable.key()).isEqualTo("#pageable.pageNumber + '-' + #pageable.pageSize");
    }

    @Test
    void findPerformanceById_메서드에_cacheNames_performanceDetail과_key_performanceId로_Cacheable이_선언되어_있다()
            throws NoSuchMethodException {
        Method method = PerformanceService.class.getMethod("findPerformanceById", Long.class);
        Cacheable cacheable = method.getAnnotation(Cacheable.class);

        assertThat(cacheable).isNotNull();
        assertThat(cacheable.cacheNames()).containsExactly("performanceDetail");
        assertThat(cacheable.key()).isEqualTo("#performanceId");
    }

    @Test
    void findPerformanceScheduleList_메서드에_cacheNames_performanceSchedules와_key_performanceId로_Cacheable이_선언되어_있다()
            throws NoSuchMethodException {
        Method method = PerformanceService.class.getMethod("findPerformanceScheduleList", Long.class);
        Cacheable cacheable = method.getAnnotation(Cacheable.class);

        assertThat(cacheable).isNotNull();
        assertThat(cacheable.cacheNames()).containsExactly("performanceSchedules");
        assertThat(cacheable.key()).isEqualTo("#performanceId");
    }

    // -------------------------------------------------------------------------
    // Fixture helpers
    // -------------------------------------------------------------------------

    private static Performance createPerformance(
            String title, String description, String posterImage,
            Integer runningTime, String ageRating, String performer) {
        Performance performance = new Performance(title, description, posterImage,
                runningTime, ageRating, performer);
        Place place = new Place("기본공연장", "서울시 강남구", 500);
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 18, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 1, 20, 0);
        Schedule schedule = new Schedule(null, place, start, start.minusDays(14),
                300, 300, end);
        performance.addSchedule(schedule);
        return performance;
    }

    private static Performance createPerformanceWithSchedule(
            String title, String posterImage,
            String placeName, LocalDateTime start, LocalDateTime end) {
        Performance performance = new Performance(title, "설명", posterImage, 120, "전체", "아티스트");
        Place place = new Place(placeName, "서울시 중구", 800);
        Schedule schedule = new Schedule(null, place, start, start.minusDays(7),
                800, 800, end);
        performance.addSchedule(schedule);
        return performance;
    }

    /**
     * JPA 엔티티의 @GeneratedValue 필드(scheduleId, performanceId 등)는 생성자로 설정할 수 없으므로
     * 리플렉션으로 주입한다.
     */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("fixture 필드 설정 실패: " + fieldName, e);
        }
    }

    private static java.lang.reflect.Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new RuntimeException("필드를 찾을 수 없음: " + name);
    }
}
