package com.example.reservationsystem;

import com.example.reservationsystem.common.enums.ErrorCode;
import com.example.reservationsystem.common.exception.CustomException;
import com.example.reservationsystem.domain.dto.SeatReservationResponseDto;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 노트북 열람실 좌석 예약 조회 유닛 테스트
 * 요구사항 명세서 3.1.2.4 기반
 *
 * 테스트 대상: SeatReservationService.getReservationsByStudentId()
 */
@ExtendWith(MockitoExtension.class)
class SeatReservationQueryUnitTest {

    @Mock
    private SeatReservationRepository seatReservationRepository;

    @InjectMocks
    private SeatReservationService seatReservationService;

    // 테스트용 상수
    private static final Long VALID_STUDENT_ID = 202111492L;
    private static final Long INVALID_STUDENT_ID_1 = 202099999L;
    private static final Long INVALID_STUDENT_ID_2 = 202288888L;

    /**
     * 테스트용 SeatReservation 객체 생성 헬퍼 메서드
     */
    private SeatReservation createMockSeatReservation(Long id, Long seatId, Long studentId,
                                                       LocalDate date, LocalTime startTime, LocalTime endTime) {
        // Seat Mock 생성
        Seat seat = mock(Seat.class);
        when(seat.getId()).thenReturn(seatId);

        // Student Mock 생성
        Student student = mock(Student.class);
        when(student.getStudentId()).thenReturn(studentId);

        // SeatReservation Mock 생성
        SeatReservation reservation = mock(SeatReservation.class);
        when(reservation.getId()).thenReturn(id);
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
        @DisplayName("TC_Q_01: 예약 1건 조회 성공")
        void TC_Q_01_예약_1건_조회_성공() {
            // Given
            SeatReservation reservation = createMockSeatReservation(
                    1L, 5L, VALID_STUDENT_ID,
                    LocalDate.of(2025, 12, 21),
                    LocalTime.of(9, 0),
                    LocalTime.of(11, 0)
            );

            when(seatReservationRepository.findByStudent_StudentId(VALID_STUDENT_ID))
                    .thenReturn(List.of(reservation));

            // When
            List<SeatReservationResponseDto> result =
                    seatReservationService.getReservationsByStudentId(VALID_STUDENT_ID);

            // Then
            assertThat(result).hasSize(1);
            verify(seatReservationRepository, times(1)).findByStudent_StudentId(VALID_STUDENT_ID);
        }

        @Test
        @DisplayName("TC_Q_02: 예약 여러 건 조회 성공")
        void TC_Q_02_예약_여러건_조회_성공() {
            // Given
            SeatReservation reservation1 = createMockSeatReservation(
                    1L, 5L, VALID_STUDENT_ID,
                    LocalDate.of(2025, 12, 21),
                    LocalTime.of(9, 0),
                    LocalTime.of(11, 0)
            );
            SeatReservation reservation2 = createMockSeatReservation(
                    2L, 10L, VALID_STUDENT_ID,
                    LocalDate.of(2025, 12, 21),
                    LocalTime.of(11, 0),
                    LocalTime.of(13, 0)
            );
            SeatReservation reservation3 = createMockSeatReservation(
                    3L, 15L, VALID_STUDENT_ID,
                    LocalDate.of(2025, 12, 22),
                    LocalTime.of(14, 0),
                    LocalTime.of(16, 0)
            );

            when(seatReservationRepository.findByStudent_StudentId(VALID_STUDENT_ID))
                    .thenReturn(List.of(reservation1, reservation2, reservation3));

            // When
            List<SeatReservationResponseDto> result =
                    seatReservationService.getReservationsByStudentId(VALID_STUDENT_ID);

            // Then
            assertThat(result).hasSize(3);
            verify(seatReservationRepository, times(1)).findByStudent_StudentId(VALID_STUDENT_ID);
        }

        @Test
        @DisplayName("TC_Q_03: 예약 없음 - 빈 리스트 반환")
        void TC_Q_03_예약_없음_빈리스트_반환() {
            // Given
            when(seatReservationRepository.findByStudent_StudentId(VALID_STUDENT_ID))
                    .thenReturn(Collections.emptyList());

            // When
            List<SeatReservationResponseDto> result =
                    seatReservationService.getReservationsByStudentId(VALID_STUDENT_ID);

            // Then
            assertThat(result).isEmpty();
            verify(seatReservationRepository, times(1)).findByStudent_StudentId(VALID_STUDENT_ID);
        }
    }

    // ===================================================================
    // 예외 경로 (Exception Path) 테스트
    // ===================================================================
    @Nested
    @DisplayName("예외 경로 테스트")
    class ExceptionPathTest {

        @Test
        @DisplayName("TC_Q_04: 학번이 null인 경우 - INVALID_STUDENT_ID 예외")
        void TC_Q_04_학번_null_예외() {
            // Given
            Long nullStudentId = null;

            // When & Then
            assertThatThrownBy(() ->
                    seatReservationService.getReservationsByStudentId(nullStudentId))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException customException = (CustomException) exception;
                        assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.INVALID_STUDENT_ID);
                    });

            // Repository 호출되지 않음 검증
            verify(seatReservationRepository, never()).findByStudent_StudentId(any());
        }

        @Test
        @DisplayName("TC_Q_05: 무효 학번 (202099999) - INVALID_STUDENT_ID 예외")
        void TC_Q_05_무효학번_202099999_예외() {
            // Given & When & Then
            assertThatThrownBy(() ->
                    seatReservationService.getReservationsByStudentId(INVALID_STUDENT_ID_1))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException customException = (CustomException) exception;
                        assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.INVALID_STUDENT_ID);
                    });

            // Repository 호출되지 않음 검증
            verify(seatReservationRepository, never()).findByStudent_StudentId(any());
        }

        @Test
        @DisplayName("TC_Q_06: 무효 학번 (202288888) - INVALID_STUDENT_ID 예외")
        void TC_Q_06_무효학번_202288888_예외() {
            // Given & When & Then
            assertThatThrownBy(() ->
                    seatReservationService.getReservationsByStudentId(INVALID_STUDENT_ID_2))
                    .isInstanceOf(CustomException.class)
                    .satisfies(exception -> {
                        CustomException customException = (CustomException) exception;
                        assertThat(customException.getErrorCode()).isEqualTo(ErrorCode.INVALID_STUDENT_ID);
                    });

            // Repository 호출되지 않음 검증
            verify(seatReservationRepository, never()).findByStudent_StudentId(any());
        }
    }

    // ===================================================================
    // 반환값 검증 테스트
    // ===================================================================
    @Nested
    @DisplayName("반환값 검증 테스트")
    class ResponseValidationTest {

        @Test
        @DisplayName("TC_Q_07: DTO 필드 매핑 검증")
        void TC_Q_07_DTO_필드_매핑_검증() {
            // Given
            Long expectedId = 100L;
            Long expectedSeatId = 25L;
            Long expectedStudentId = VALID_STUDENT_ID;
            LocalDate expectedDate = LocalDate.of(2025, 12, 21);
            LocalTime expectedStartTime = LocalTime.of(9, 0);
            LocalTime expectedEndTime = LocalTime.of(11, 0);

            SeatReservation reservation = createMockSeatReservation(
                    expectedId, expectedSeatId, expectedStudentId,
                    expectedDate, expectedStartTime, expectedEndTime
            );

            when(seatReservationRepository.findByStudent_StudentId(VALID_STUDENT_ID))
                    .thenReturn(List.of(reservation));

            // When
            List<SeatReservationResponseDto> result =
                    seatReservationService.getReservationsByStudentId(VALID_STUDENT_ID);

            // Then
            assertThat(result).hasSize(1);

            SeatReservationResponseDto dto = result.get(0);
            assertThat(dto.getId()).isEqualTo(expectedId);
            assertThat(dto.getSeatId()).isEqualTo(expectedSeatId.intValue());
            assertThat(dto.getStudentId()).isEqualTo(expectedStudentId);
            assertThat(dto.getDate()).isEqualTo(expectedDate);
            assertThat(dto.getStartTime()).isEqualTo(expectedStartTime);
            assertThat(dto.getEndTime()).isEqualTo(expectedEndTime);
        }
    }
}
