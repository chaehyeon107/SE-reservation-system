package com.example.reservationsystem;

import com.example.reservationsystem.domain.entity.Room;
import com.example.reservationsystem.domain.entity.Seat;
import com.example.reservationsystem.domain.repository.RoomRepository;
import com.example.reservationsystem.domain.repository.RoomReservationParticipantRepository;
import com.example.reservationsystem.domain.repository.RoomReservationRepository;
import com.example.reservationsystem.domain.repository.SeatRepository;
import com.example.reservationsystem.domain.repository.SeatReservationRepository;
import com.example.reservationsystem.domain.repository.StudentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class InvalidStudentIdUseCaseComponentTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired private WebApplicationContext wac;

    @Autowired RoomRepository roomRepository;
    @Autowired RoomReservationRepository roomReservationRepository;
    @Autowired RoomReservationParticipantRepository roomReservationParticipantRepository;

    @Autowired SeatRepository seatRepository;
    @Autowired SeatReservationRepository seatReservationRepository;

    @Autowired StudentRepository studentRepository;

    private Long roomId;

    private static final long LEADER = 202111492L;
    private static final long P1 = 202213007L;
    private static final long P2 = 202212121L;

    @BeforeEach
    void setup() {
        this.mockMvc = webAppContextSetup(wac).build();
        roomReservationParticipantRepository.deleteAllInBatch();
        roomReservationRepository.deleteAllInBatch();
        seatReservationRepository.deleteAllInBatch();
        studentRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();

        Room room = roomRepository.save(Room.builder().capacity(3).build());
        roomId = room.getId();

        if (seatRepository.count() == 0) {
            IntStream.rangeClosed(1, 70).forEach(i ->
                    seatRepository.save(Seat.builder().seatNumber(i).status("AVAILABLE").build())
            );
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {202099999L, 202288888L})
    @DisplayName("UC-AUTH-ROOM-01 회의실 예약 생성: 잘못된 학번(대표자) 차단 -> INVALID_STUDENT_ID")
    void UC_AUTH_ROOM_01(long badId) throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);

        mockMvc.perform(post("/api/meeting/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "roomId": %d,
                              "date": "%s",
                              "startTime": "10:00",
                              "duration": 2,
                              "representativeStudentId": %d,
                              "participantStudentIds": [%d, %d]
                            }
                            """.formatted(roomId, date, badId, P1, P2)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_STUDENT_ID"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 학번입니다."));

        assertThat(roomReservationRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(longs = {202099999L, 202288888L})
    @DisplayName("UC-AUTH-ROOM-02 회의실 예약 생성: 잘못된 학번(참가자 포함) 차단 -> INVALID_STUDENT_ID")
    void UC_AUTH_ROOM_02(long badId) throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);

        mockMvc.perform(post("/api/meeting/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "roomId": %d,
                              "date": "%s",
                              "startTime": "10:00",
                              "duration": 2,
                              "representativeStudentId": %d,
                              "participantStudentIds": [%d, %d]
                            }
                            """.formatted(roomId, date, LEADER, badId, P1)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_STUDENT_ID"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 학번입니다."));

        assertThat(roomReservationRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(longs = {202099999L, 202288888L})
    @DisplayName("UC-AUTH-ROOM-03 회의실 내 예약 조회: 잘못된 학번 차단 -> INVALID_STUDENT_ID")
    void UC_AUTH_ROOM_03(long badId) throws Exception {
        mockMvc.perform(get("/api/meeting/reservations")
                        .param("studentId", String.valueOf(badId)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_STUDENT_ID"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 학번입니다."));
    }

    @ParameterizedTest
    @ValueSource(longs = {202099999L, 202288888L})
    @DisplayName("UC-AUTH-SEAT-01 열람실(좌석 지정) 예약: 잘못된 학번 차단 -> INVALID_STUDENT_ID")
    void UC_AUTH_SEAT_01(long badId) throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);

        mockMvc.perform(post("/api/seats/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "studentId": %d,
                              "seatId": 1,
                              "date": "%s",
                              "startTime": "09:00",
                              "durationHours": 2
                            }
                            """.formatted(badId, date)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_STUDENT_ID"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 학번입니다."));

        assertThat(seatReservationRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(longs = {202099999L, 202288888L})
    @DisplayName("UC-AUTH-SEAT-02 열람실(랜덤 좌석) 예약: 잘못된 학번 차단 -> INVALID_STUDENT_ID")
    void UC_AUTH_SEAT_02(long badId) throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);

        mockMvc.perform(post("/api/seats/reservations/random")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "studentId": %d,
                              "seatId": 0,
                              "date": "%s",
                              "startTime": "09:00",
                              "durationHours": 2
                            }
                            """.formatted(badId, date)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_STUDENT_ID"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 학번입니다."));

        assertThat(seatReservationRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(longs = {202099999L, 202288888L})
    @DisplayName("UC-AUTH-SEAT-03 내 좌석 예약 조회: 잘못된 학번 차단 -> INVALID_STUDENT_ID")
    void UC_AUTH_SEAT_03(long badId) throws Exception {
        mockMvc.perform(get("/api/seats/reservations")
                        .param("studentId", String.valueOf(badId)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_STUDENT_ID"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 학번입니다."));
    }
}
