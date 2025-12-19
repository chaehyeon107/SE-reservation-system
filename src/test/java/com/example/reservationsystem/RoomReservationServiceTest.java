package com.example.reservationsystem.domain.service;

import com.example.reservationsystem.common.enums.ErrorCode;
import com.example.reservationsystem.common.exception.CustomException;
import com.example.reservationsystem.domain.dto.ReservationRequestDto;
import com.example.reservationsystem.domain.entity.Room;
import com.example.reservationsystem.domain.entity.RoomReservation;
import com.example.reservationsystem.domain.entity.RoomReservationStatus;
import com.example.reservationsystem.domain.entity.Student;
import com.example.reservationsystem.domain.repository.RoomRepository;
import com.example.reservationsystem.domain.repository.RoomReservationParticipantRepository;
import com.example.reservationsystem.domain.repository.RoomReservationRepository;
import com.example.reservationsystem.domain.repository.StudentRepository;
import com.example.reservationsystem.testsupport.TcLogWatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(TcLogWatcher.class)
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class RoomReservationServiceTest {

    @Autowired RoomReservationService roomReservationService;
    @Autowired RoomReservationRepository roomReservationRepository;
    @Autowired RoomReservationParticipantRepository roomReservationParticipantRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired StudentRepository studentRepository;

    private static final Long LEADER = 20250001L;
    private static final Long P1 = 20250002L;
    private static final Long P2 = 20250003L;
    private static final Long OUTSIDER = 20259999L;

    private Room room;

    @BeforeEach
    void setUp() {
        // ✅ FK 순서 중요
        roomReservationParticipantRepository.deleteAll();
        roomReservationRepository.deleteAll();
        studentRepository.deleteAll();
        roomRepository.deleteAll();

        room = roomRepository.save(Room.builder().capacity(3).build());

        studentRepository.saveAll(List.of(
                Student.of(LEADER),
                Student.of(P1),
                Student.of(P2),
                Student.of(OUTSIDER)
        ));
    }

    private Long createReservation(LocalDate date, LocalTime startTime) {
        ReservationRequestDto req = new ReservationRequestDto(
                room.getId(),
                date,
                startTime,
                2,
                LEADER,
                List.of(P1, P2)
        );
        return roomReservationService.createRoomReservation(req).getId();
    }

    // =========================================================
    // S-ROOM-CANCEL (UNIT)
    // =========================================================

    @Test
    @DisplayName("UT-ROOM-C-01 대표자 취소 성공 (시작 전 → 환급)")
    void UT_ROOM_C_01() {
        Long reservationId = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0));

        assertThatNoException()
                .isThrownBy(() -> roomReservationService.cancelMeetingReservation(reservationId, LEADER));

        RoomReservation canceled = roomReservationRepository.findById(reservationId).orElseThrow();
        assertThat(canceled.getStatus()).isEqualTo(RoomReservationStatus.CANCELED_REFUND);

        // ✅ 참가자 전원 환급(-2) → 0
        assertThat(studentRepository.findByStudentId(LEADER).orElseThrow().getMeetingDailyUsedHours()).isEqualTo(0);
        assertThat(studentRepository.findByStudentId(P1).orElseThrow().getMeetingDailyUsedHours()).isEqualTo(0);
        assertThat(studentRepository.findByStudentId(P2).orElseThrow().getMeetingDailyUsedHours()).isEqualTo(0);
    }

    @Test
    @DisplayName("UT-ROOM-C-02 대표자 취소 성공 (시작 후 → 페널티)")
    void UT_ROOM_C_02() {
        Long reservationId = createReservation(LocalDate.now().minusDays(1), LocalTime.of(10, 0));

        assertThatNoException()
                .isThrownBy(() -> roomReservationService.cancelMeetingReservation(reservationId, LEADER));

        RoomReservation canceled = roomReservationRepository.findById(reservationId).orElseThrow();
        assertThat(canceled.getStatus()).isEqualTo(RoomReservationStatus.CANCELED_PENALTY);

        // ✅ create(+2) + penalty(+2) = 4 (참가자 전원)
        assertThat(studentRepository.findByStudentId(LEADER).orElseThrow().getMeetingDailyUsedHours()).isEqualTo(4);
        assertThat(studentRepository.findByStudentId(P1).orElseThrow().getMeetingDailyUsedHours()).isEqualTo(4);
        assertThat(studentRepository.findByStudentId(P2).orElseThrow().getMeetingDailyUsedHours()).isEqualTo(4);
    }

    @Test
    @DisplayName("UT-ROOM-C-03 비대표자 취소 → NO_CANCEL_PERMISSION")
    void UT_ROOM_C_03() {
        Long reservationId = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0));

        assertThatThrownBy(() -> roomReservationService.cancelMeetingReservation(reservationId, P1))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.NO_CANCEL_PERMISSION));
    }

    @Test
    @DisplayName("UT-ROOM-C-04 예약과 무관한 학생 취소 → NO_CANCEL_PERMISSION")
    void UT_ROOM_C_04() {
        Long reservationId = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0));

        assertThatThrownBy(() -> roomReservationService.cancelMeetingReservation(reservationId, OUTSIDER))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.NO_CANCEL_PERMISSION));
    }

    @Test
    @DisplayName("UT-ROOM-C-05 없는 예약 취소 → RESERVATION_NOT_FOUND")
    void UT_ROOM_C_05() {
        assertThatThrownBy(() -> roomReservationService.cancelMeetingReservation(999999L, LEADER))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESERVATION_NOT_FOUND));
    }

    @Test
    @DisplayName("UT-ROOM-C-06 취소 후 재취소 → ALREADY_CANCELED_RESERVATION")
    void UT_ROOM_C_06() {
        Long reservationId = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0));

        roomReservationService.cancelMeetingReservation(reservationId, LEADER);

        assertThatThrownBy(() -> roomReservationService.cancelMeetingReservation(reservationId, LEADER))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_CANCELED_RESERVATION));
    }

    @Test
    @DisplayName("UT-ROOM-C-07 무효 학번 취소 → INVALID_STUDENT_ID")
    void UT_ROOM_C_07() {
        Long reservationId = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0));

        assertThatThrownBy(() -> roomReservationService.cancelMeetingReservation(reservationId, 202099999L))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_STUDENT_ID));
    }

    // =========================================================
    // S-ROOM-LIST (UNIT)
    // =========================================================

    @Test
    @DisplayName("UT-ROOM-L-01 내 예약 목록 조회(있음)")
    void UT_ROOM_L_01() {
        Long reservationId = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0));

        var list = roomReservationService.getReservationsByStudentId(LEADER);

        assertThat(list).isNotEmpty();
        assertThat(list.stream().anyMatch(x -> x.getId().equals(reservationId))).isTrue();
    }

    @Test
    @DisplayName("UT-ROOM-L-02 내 예약 목록 조회(없음)")
    void UT_ROOM_L_02() {
        var list = roomReservationService.getReservationsByStudentId(LEADER);
        assertThat(list).isEmpty();
    }

    @Test
    @DisplayName("UT-ROOM-L-03 취소 후 조회 반영(취소 예약 제외)")
    void UT_ROOM_L_03() {
        Long reservationId = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0));
        roomReservationService.cancelMeetingReservation(reservationId, LEADER);

        var list = roomReservationService.getReservationsByStudentId(LEADER);
        assertThat(list).isEmpty();
    }

    @Test
    @DisplayName("UT-ROOM-L-04 무효 학번 조회 → INVALID_STUDENT_ID")
    void UT_ROOM_L_04() {
        assertThatThrownBy(() -> roomReservationService.getReservationsByStudentId(202099999L))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_STUDENT_ID));
    }
}
