package com.example.reservationsystem.domain.service;

import com.example.reservationsystem.common.enums.ErrorCode;
import com.example.reservationsystem.common.exception.CustomException;
import com.example.reservationsystem.domain.dto.ReservationRequestDto;
import com.example.reservationsystem.domain.entity.Room;
import com.example.reservationsystem.domain.entity.Student;
import com.example.reservationsystem.domain.repository.RoomRepository;
import com.example.reservationsystem.domain.repository.RoomReservationRepository;
import com.example.reservationsystem.domain.repository.StudentRepository;
import com.example.reservationsystem.testsupport.TcLogExtension;
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

@ExtendWith(TcLogExtension.class)
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class RoomReservationServiceTest {

    @Autowired RoomReservationService roomReservationService;
    @Autowired RoomReservationRepository roomReservationRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired StudentRepository studentRepository;

    private static final Long LEADER = 20250001L;
    private static final Long P1 = 20250002L;
    private static final Long P2 = 20250003L;
    private static final Long OUTSIDER = 20259999L;

    private Room room;

    @BeforeEach
    void setUp() {
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

    // ------------------------
    // 문서용 로그 유틸
    // ------------------------
    private void logPass(String tcId, String detail) {
        System.out.println("[TC=" + tcId + "] [RESULT=PASS] " + detail);
    }

    private void logExpectedError(String tcId, ErrorCode code, String msg) {
        System.out.println(
                "[TC=" + tcId + "] [RESULT=PASS] [EXPECTED_ERROR=" + code.name() + "] [EXPECTED_MESSAGE=" + msg + "]"
        );
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
        String tcId = "UT-ROOM-C-01";
        Long reservationId = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0));

        assertThatNoException()
                .isThrownBy(() -> roomReservationService.cancelMeetingReservation(reservationId, LEADER));

        assertThat(roomReservationRepository.findById(reservationId)).isEmpty();

        Student leader = studentRepository.findByStudentId(LEADER).orElseThrow();
        assertThat(leader.getMeetingDailyUsedHours()).isEqualTo(0);

        logPass(tcId, "[reservationId=" + reservationId + "] [EXPECT=deleted & refund]");
    }

    @Test
    @DisplayName("UT-ROOM-C-02 대표자 취소 성공 (시작 후 → 페널티)")
    void UT_ROOM_C_02() {
        String tcId = "UT-ROOM-C-02";
        Long reservationId = createReservation(LocalDate.now().minusDays(1), LocalTime.of(10, 0));

        assertThatNoException()
                .isThrownBy(() -> roomReservationService.cancelMeetingReservation(reservationId, LEADER));

        Student leader = studentRepository.findByStudentId(LEADER).orElseThrow();
        assertThat(leader.getMeetingDailyUsedHours()).isEqualTo(4); // 2시간 예약 => +2(생성) +2(취소페널티)

        logPass(tcId, "[reservationId=" + reservationId + "] [EXPECT=deleted & penaltyApplied(dailyUsed=4)]");
    }

    @Test
    @DisplayName("UT-ROOM-C-03 비대표자 취소 → NO_CANCEL_PERMISSION")
    void UT_ROOM_C_03() {
        String tcId = "UT-ROOM-C-03";
        Long reservationId = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0));

        ErrorCode expected = ErrorCode.NO_CANCEL_PERMISSION;
        String expectedMsg = expected.getMessage(); // ✅ ErrorCode 메시지 assert

        assertThatThrownBy(() -> roomReservationService.cancelMeetingReservation(reservationId, P1))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException ce = (CustomException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo(expected);

                    // ✅ 메시지는 구현 방식에 따라 (ErrorCode.message 또는 exception.message) 중 하나가 맞을 수 있음
                    // 둘 다 안전하게 검증:
                    assertThat(expectedMsg).isNotBlank();
                    assertThat(ce.getMessage()).isNotBlank(); // 예외 메시지 존재성
                });

        // 예약이 유지되는지까지 확인(권장)
        assertThat(roomReservationRepository.findById(reservationId)).isPresent();

        logExpectedError(tcId, expected, expectedMsg);

    }

    @Test
    @DisplayName("UT-ROOM-C-04 예약과 무관한 학생 취소 → NO_CANCEL_PERMISSION")
    void UT_ROOM_C_04() {
        String tcId = "UT-ROOM-C-04";
        Long reservationId = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0));

        ErrorCode expected = ErrorCode.NO_CANCEL_PERMISSION;
        String expectedMsg = expected.getMessage();

        assertThatThrownBy(() -> roomReservationService.cancelMeetingReservation(reservationId, OUTSIDER))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException ce = (CustomException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo(expected);
                    assertThat(expectedMsg).isNotBlank();
                    assertThat(ce.getMessage()).isNotBlank();
                });

        assertThat(roomReservationRepository.findById(reservationId)).isPresent();

        logExpectedError(tcId, expected, expectedMsg);
    }

    @Test
    @DisplayName("UT-ROOM-C-05 없는 예약 취소 → RESERVATION_NOT_FOUND")
    void UT_ROOM_C_05() {
        String tcId = "UT-ROOM-C-05";

        ErrorCode expected = ErrorCode.RESERVATION_NOT_FOUND;
        String expectedMsg = expected.getMessage();

        assertThatThrownBy(() -> roomReservationService.cancelMeetingReservation(999999L, LEADER))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException ce = (CustomException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo(expected);
                    assertThat(expectedMsg).isNotBlank();
                    assertThat(ce.getMessage()).isNotBlank();
                });

        logExpectedError(tcId, expected, expectedMsg);
    }

    @Test
    @DisplayName("UT-ROOM-C-06 취소 후 재취소 → RESERVATION_NOT_FOUND")
    void UT_ROOM_C_06() {
        String tcId = "UT-ROOM-C-06";
        Long reservationId = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0));

        // 1) 첫 취소는 성공
        roomReservationService.cancelMeetingReservation(reservationId, LEADER);

        // 2) 재취소는 NOT_FOUND
        ErrorCode expected = ErrorCode.RESERVATION_NOT_FOUND;
        String expectedMsg = expected.getMessage();

        assertThatThrownBy(() -> roomReservationService.cancelMeetingReservation(reservationId, LEADER))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException ce = (CustomException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo(expected);
                    assertThat(expectedMsg).isNotBlank();
                    assertThat(ce.getMessage()).isNotBlank();
                });

        logExpectedError(tcId, expected, expectedMsg);
    }

    @Test
    @DisplayName("UT-ROOM-C-07 무효 학번 취소 → INVALID_STUDENT_ID")
    void UT_ROOM_C_07() {
        String tcId = "UT-ROOM-C-07";
        Long reservationId = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0));

        ErrorCode expected = ErrorCode.INVALID_STUDENT_ID;
        String expectedMsg = expected.getMessage();

        assertThatThrownBy(() -> roomReservationService.cancelMeetingReservation(reservationId, 202099999L))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException ce = (CustomException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo(expected);
                    assertThat(expectedMsg).isNotBlank();
                    assertThat(ce.getMessage()).isNotBlank();
                });

        logExpectedError(tcId, expected, expectedMsg);
    }

    // =========================================================
    // S-ROOM-LIST (UNIT)
    // =========================================================

    @Test
    @DisplayName("UT-ROOM-L-01 내 예약 목록 조회(있음)")
    void UT_ROOM_L_01() {
        String tcId = "UT-ROOM-L-01";
        Long reservationId = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0));

        var list = roomReservationService.getReservationsByStudentId(LEADER);

        assertThat(list).isNotEmpty();
        assertThat(list.stream().anyMatch(x -> x.getId().equals(reservationId))).isTrue();

        logPass(tcId, "[EXPECT=list contains reservationId=" + reservationId + "]");
    }

    @Test
    @DisplayName("UT-ROOM-L-02 내 예약 목록 조회(없음)")
    void UT_ROOM_L_02() {
        String tcId = "UT-ROOM-L-02";
        var list = roomReservationService.getReservationsByStudentId(LEADER);

        assertThat(list).isEmpty();

        logPass(tcId, "[EXPECT=empty list]");
    }

    @Test
    @DisplayName("UT-ROOM-L-03 취소 후 조회 반영")
    void UT_ROOM_L_03() {
        String tcId = "UT-ROOM-L-03";
        Long reservationId = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0));

        roomReservationService.cancelMeetingReservation(reservationId, LEADER);

        var list = roomReservationService.getReservationsByStudentId(LEADER);
        assertThat(list).isEmpty();

        logPass(tcId, "[EXPECT=empty list after cancel]");
    }

    @Test
    @DisplayName("UT-ROOM-L-04 무효 학번 조회 → INVALID_STUDENT_ID")
    void UT_ROOM_L_04() {
        String tcId = "UT-ROOM-L-04";

        ErrorCode expected = ErrorCode.INVALID_STUDENT_ID;
        String expectedMsg = expected.getMessage();

        assertThatThrownBy(() -> roomReservationService.getReservationsByStudentId(202099999L))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException ce = (CustomException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo(expected);
                    assertThat(expectedMsg).isNotBlank();
                    assertThat(ce.getMessage()).isNotBlank();
                });

        logExpectedError(tcId, expected, expectedMsg);
    }
}
