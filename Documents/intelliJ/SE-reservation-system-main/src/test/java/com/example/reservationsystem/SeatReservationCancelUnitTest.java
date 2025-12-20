package com.example.reservationsystem;

import com.example.reservationsystem.common.enums.ErrorCode;
import com.example.reservationsystem.common.exception.CustomException;
import com.example.reservationsystem.domain.entity.Seat;
import com.example.reservationsystem.domain.entity.SeatReservation;
import com.example.reservationsystem.domain.entity.Student;
import com.example.reservationsystem.domain.repository.SeatReservationRepository;
import com.example.reservationsystem.domain.service.SeatReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 노트북 열람실 좌석 예약 취소 유닛 테스트
 * 요구사항 명세서 3.1.2.3 기반
 *
 * 테스트 대상: SeatReservationService.cancelSeatReservation()
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SeatReservationCancelUnitTest {

    @Mock
    private SeatReservationRepository seatReservationRepository;

    @InjectMocks
    private SeatReservationService seatReservationService;

    // 테스트용 상수
    private static final Long VALID_STUDENT_ID = 202111492L;
    private static final Long OTHER_STUDENT_ID = 202213007L;
    private static final Long INVALID_STUDENT_ID_1 = 202099999L;
    private static final Long INVALID_STUDENT_ID_2 = 202288888L;
    private static final Long RESERVATION_ID = 1L;

    /**
     * 미래 날짜의 예약 Mock 생성 (취소 가능)
     */
    private SeatReservation createFutureReservationMock(Long reservationId, Long studentId) {
        // 미래 날짜 (내일)
        LocalDate futureDate = LocalDate.now().plusDays(1);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(11, 0);

        return createReservationMock(reservationId, studentId, futureDate, startTime, endTime);
    }

    /**
     * 과거 날짜의 예약 Mock 생성 (취소 불가 - 이미 시작됨)
     */
    private SeatReservation createPastReservationMock(Long reservationId, Long studentId) {
        // 과거 날짜 (어제)
        LocalDate pastDate = LocalDate.now().minusDays(1);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(11, 0);

        return createReservationMock(reservationId, studentId, pastDate, startTime, endTime);
    }

    /**
     * 예약 Mock 생성 헬퍼
     */
    private SeatReservation createReservationMock(Long reservationId, Long studentId,
                                                   LocalDate date, LocalTime startTime, LocalTime endTime) {
        // Seat Mock
        Seat seat = mock(Seat.class);
        when(seat.getId()).thenReturn(5L);

        // Student Mock (spy 사용하여 실제 메서드 호출 가능)
        Student student = spy(Student.builder()
                .id(1L)
                .studentId(studentId)
                .seatDailyUsedHours(2)
                .build());

        // SeatReservation Mock
        SeatReservation reservation = mock(SeatReservation.class);
        when(reservation.getId()).thenReturn(reservationId);
        when(reservation.getSeat()).thenReturn(seat);
        when(reservation.getStudent()).thenReturn(student);
        when(reservation.getDate()).thenReturn(date);
        when(reservation.getStartTime()).thenReturn(startTime);
        when(reservation.getEndTime()).thenReturn(endTime);

        return reservation;
    }

    // ===================================================================
    // 기본 경로 (Basic Path) 테스트
    // ===================================================================
    @Nested
    @DisplayName("기본 경로 테스트")
    class BasicPathTest {

        @Test
        @DisplayName("TC_C_01: 예약 시작 전 취소 성공 - '시간 환급' 반환")
        void TC_C_01_예약시작전_취소성공_시간환급() {
            // Given
            SeatReservation reservation = createFutureReservationMock(RESERVATION_ID, VALID_STUDENT_ID);
            when(seatReservationRepository.findById(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));

            // When
            String result = seatReservationService.cancelSeatReservation(RESERVATION_ID, VALID_STUDENT_ID);

            // Then
            assertThat(result).isEqualTo("시간 환급");
        }

        @Test
        @DisplayName("TC_C_02: 취소 시 예약 삭제(delete) 호출 확인")
        void TC_C_02_취소시_예약삭제_호출확인() {
            // Given
            SeatReservation reservation = createFutureReservationMock(RESERVATION_ID, VALID_STUDENT_ID);
            when(seatReservationRepository.findById(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));

            // When
            seatReservationService.cancelSeatReservation(RESERVATION_ID, VALID_STUDENT_ID);

            // Then
            verify(seatReservationRepository, times(1)).delete(reservation);
        }

        @Test
        @DisplayName("TC_C_03: 취소 시 사용 시간 환급 확인 (applySeatUsageDelta 호출)")
        void TC_C_03_취소시_사용시간_환급확인() {
            // Given
            SeatReservation reservation = createFutureReservationMock(RESERVATION_ID, VALID_STUDENT_ID);
            Student student = reservation.getStudent();

            when(seatReservationRepository.findById(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));

            // When
            seatReservationService.cancelSeatReservation(RESERVATION_ID, VALID_STUDENT_ID);

            // Then: 2시간 예약이므로 -2 환급
            verify(student, times(1)).applySeatUsageDelta(-2);
        }
    }

    // ===================================================================
    // 예외 경로 (Exception Path) 테스트 - 학번 검증
    // ===================================================================
    @Nested
    @DisplayName("예외 경로 테스트 - 학번 검증")
    class StudentIdValidationTest {

        @Test
        @DisplayName("TC_C_04: 학번이 null인 경우 - INVALID_STUDENT_ID 예외")
        void TC_C_04_학번_null_예외() {
            // Given
            Long nullStudentId = null;

            // When & Then
            assertThatThrownBy(() ->
                    seatReservationService.cancelSeatReservation(RESERVATION_ID, nullStudentId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException customException = (CustomException) exception;
                        assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.INVALID_STUDENT_ID);
                    });

            // Repository 호출되지 않음
            verify(seatReservationRepository, never()).findById(any());
        }

        @Test
        @DisplayName("TC_C_05: 무효 학번 (202099999) - INVALID_STUDENT_ID 예외")
        void TC_C_05_무효학번_202099999_예외() {
            // When & Then
            assertThatThrownBy(() ->
                    seatReservationService.cancelSeatReservation(RESERVATION_ID, INVALID_STUDENT_ID_1))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException customException = (CustomException) exception;
                        assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.INVALID_STUDENT_ID);
                    });

            verify(seatReservationRepository, never()).findById(any());
        }

        @Test
        @DisplayName("TC_C_06: 무효 학번 (202288888) - INVALID_STUDENT_ID 예외")
        void TC_C_06_무효학번_202288888_예외() {
            // When & Then
            assertThatThrownBy(() ->
                    seatReservationService.cancelSeatReservation(RESERVATION_ID, INVALID_STUDENT_ID_2))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException customException = (CustomException) exception;
                        assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.INVALID_STUDENT_ID);
                    });

            verify(seatReservationRepository, never()).findById(any());
        }
    }

    // ===================================================================
    // 예외 경로 (Exception Path) 테스트 - 예약 검증
    // ===================================================================
    @Nested
    @DisplayName("예외 경로 테스트 - 예약 검증")
    class ReservationValidationTest {

        @Test
        @DisplayName("TC_C_07: 존재하지 않는 예약 ID - SEAT_RESERVATION_NOT_FOUND1 예외")
        void TC_C_07_존재하지않는_예약_예외() {
            // Given
            Long nonExistentId = 99999L;
            when(seatReservationRepository.findById(nonExistentId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    seatReservationService.cancelSeatReservation(nonExistentId, VALID_STUDENT_ID))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException customException = (CustomException) exception;
                        assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.SEAT_RESERVATION_NOT_FOUND1);
                    });

            // delete 호출되지 않음
            verify(seatReservationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("TC_C_08: 타인 예약 취소 시도 - NO_CANCEL_PERMISSION1 예외")
        void TC_C_08_타인예약_취소시도_권한없음_예외() {
            // Given: VALID_STUDENT_ID의 예약을 OTHER_STUDENT_ID가 취소 시도
            SeatReservation reservation = createFutureReservationMock(RESERVATION_ID, VALID_STUDENT_ID);
            when(seatReservationRepository.findById(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));

            // When & Then
            assertThatThrownBy(() ->
                    seatReservationService.cancelSeatReservation(RESERVATION_ID, OTHER_STUDENT_ID))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException customException = (CustomException) exception;
                        assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.NO_CANCEL_PERMISSION1);
                    });

            // delete 호출되지 않음
            verify(seatReservationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("TC_C_09: 이미 시작된 예약 취소 시도 - SEAT_ALREADY_IN_USE 예외")
        void TC_C_09_이미시작된_예약_취소불가_예외() {
            // Given: 과거 날짜의 예약 (이미 시작됨)
            SeatReservation reservation = createPastReservationMock(RESERVATION_ID, VALID_STUDENT_ID);
            when(seatReservationRepository.findById(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));

            // When & Then
            assertThatThrownBy(() ->
                    seatReservationService.cancelSeatReservation(RESERVATION_ID, VALID_STUDENT_ID))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException customException = (CustomException) exception;
                        assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.SEAT_ALREADY_IN_USE);
                    });

            // delete 호출되지 않음
            verify(seatReservationRepository, never()).delete(any());
        }
    }
}
