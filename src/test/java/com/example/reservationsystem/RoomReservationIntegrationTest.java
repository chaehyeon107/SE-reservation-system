package com.example.reservationsystem;

import com.example.reservationsystem.domain.entity.Room;
import com.example.reservationsystem.domain.entity.Student;
import com.example.reservationsystem.domain.repository.RoomRepository;
import com.example.reservationsystem.domain.repository.RoomReservationParticipantRepository;
import com.example.reservationsystem.domain.repository.RoomReservationRepository;
import com.example.reservationsystem.domain.repository.StudentRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ✅ 회의실 예약 "조회(내 예약)" + "취소" 통합테스트
 * - 프론트 없이 MockMvc로 API 호출
 * - H2(DB) 기반으로 Repository 상태/Student 사용시간까지 검증
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class RoomReservationIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired RoomRepository roomRepository;
    @Autowired RoomReservationRepository roomReservationRepository;
    @Autowired RoomReservationParticipantRepository roomReservationParticipantRepository;
    @Autowired StudentRepository studentRepository;

    private Long roomId;

    private static final long LEADER = 202212121L;
    private static final long P1 = 202111492L;
    private static final long P2 = 202213007L;
    private static final long OUTSIDER = 202300001L;

    @BeforeEach
    void setup() {
        // FK 순서 고려해서 정리
        roomReservationParticipantRepository.deleteAll();
        roomReservationRepository.deleteAll();
        studentRepository.deleteAll();
        roomRepository.deleteAll();

        Room room = roomRepository.save(Room.builder().capacity(3).build());
        roomId = room.getId();
    }

    /** 예약 생성 helper: 성공 시 reservationId 반환 */
    private long createReservation(LocalDate date, LocalTime start, int duration) throws Exception {
        String body = """
            {
              "roomId": %d,
              "date": "%s",
              "startTime": "%s",
              "duration": %d,
              "representativeStudentId": %d,
              "participantStudentIds": [%d, %d]
            }
            """.formatted(roomId, date, start, duration, LEADER, P1, P2);

        String res = mockMvc.perform(post("/api/meeting/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("회의실 예약이 생성되었습니다."))
                .andExpect(jsonPath("$.payload.id").exists())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(res);
        return root.path("payload").path("id").asLong();
    }

    private Student mustFindStudent(long studentId) {
        return studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new AssertionError("Student not found: " + studentId));
    }

    // ----------------------------------------------------------------------
    // S-ROOM-LIST
    // ----------------------------------------------------------------------

    @Test
    void TC_ROOM_L_01_예약목록조회_있음() throws Exception {
        long id = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0), 2);

        mockMvc.perform(get("/api/meeting/reservations")
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("회의실 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(result -> {
                    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
                    boolean found = false;
                    for (JsonNode item : root.path("payload")) {
                        if (item.path("id").asLong() == id) { found = true; break; }
                    }
                    assertThat(found).isTrue();
                });
    }

    @Test
    void TC_ROOM_L_02_예약목록조회_없음() throws Exception {
        mockMvc.perform(get("/api/meeting/reservations")
                        .param("studentId", "202233333"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("회의실 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload").isEmpty());
    }

    @Test
    void TC_ROOM_L_04_잘못된요청_파라미터없음_400() throws Exception {
        mockMvc.perform(get("/api/meeting/reservations"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void TC_ROOM_L_03_취소후목록반영_즉시삭제확인() throws Exception {
        long id = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0), 2);

        mockMvc.perform(delete("/api/meeting/reservations/{id}", id)
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("예약이 정상적으로 취소되었습니다."));

        // 목록에서 사라져야 함(구현이 delete)
        mockMvc.perform(get("/api/meeting/reservations")
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
                    for (JsonNode item : root.path("payload")) {
                        assertThat(item.path("id").asLong()).isNotEqualTo(id);
                    }
                });

        // DB에서도 삭제
        assertThat(roomReservationRepository.findById(id)).isEmpty();
    }

    // ----------------------------------------------------------------------
    // S-ROOM-CANCEL
    // ----------------------------------------------------------------------

    @Test
    void TC_ROOM_C_01_대표자취소_시작전_환급_삭제확인() throws Exception {
        // 미래 예약 -> 시작 전(beforeStart=true)
        LocalDate date = LocalDate.now().plusDays(1);
        long id = createReservation(date, LocalTime.of(10, 0), 2);

        // 생성 시점: 참가자 전원 +2
        assertThat(mustFindStudent(LEADER).getMeetingDailyUsedHours()).isEqualTo(2);
        assertThat(mustFindStudent(P1).getMeetingDailyUsedHours()).isEqualTo(2);
        assertThat(mustFindStudent(P2).getMeetingDailyUsedHours()).isEqualTo(2);

        mockMvc.perform(delete("/api/meeting/reservations/{id}", id)
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("예약이 정상적으로 취소되었습니다."))
                .andExpect(jsonPath("$.payload").doesNotExist());

        // ✅ 삭제 구현이므로 DB에서 없어야 함
        assertThat(roomReservationRepository.findById(id)).isEmpty();

        // ✅ 시작 전 취소는 "환급(-)" -> 0으로 복구
        assertThat(mustFindStudent(LEADER).getMeetingDailyUsedHours()).isEqualTo(0);
        assertThat(mustFindStudent(P1).getMeetingDailyUsedHours()).isEqualTo(0);
        assertThat(mustFindStudent(P2).getMeetingDailyUsedHours()).isEqualTo(0);
    }

    @Test
    void TC_ROOM_C_02_대표자취소_시작후_페널티_삭제확인() throws Exception {
        // 과거 예약 -> 시작 후(beforeStart=false)
        LocalDate date = LocalDate.now().minusDays(1);
        long id = createReservation(date, LocalTime.of(10, 0), 2);

        // 생성 시점: +2
        assertThat(mustFindStudent(LEADER).getMeetingDailyUsedHours()).isEqualTo(2);

        mockMvc.perform(delete("/api/meeting/reservations/{id}", id)
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("예약이 정상적으로 취소되었습니다."));

        // ✅ 삭제 구현
        assertThat(roomReservationRepository.findById(id)).isEmpty();

        // ✅ 시작 후 취소는 "차감(+)" -> create(+2) + cancel(+2) = 4
        assertThat(mustFindStudent(LEADER).getMeetingDailyUsedHours()).isEqualTo(4);
        assertThat(mustFindStudent(P1).getMeetingDailyUsedHours()).isEqualTo(4);
        assertThat(mustFindStudent(P2).getMeetingDailyUsedHours()).isEqualTo(4);
    }

    @Test
    void TC_ROOM_C_03_비대표자취소_403_NO_CANCEL_PERMISSION() throws Exception {
        long id = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0), 2);

        mockMvc.perform(delete("/api/meeting/reservations/{id}", id)
                        .param("studentId", String.valueOf(P1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("NO_CANCEL_PERMISSION"));

        // 예약 유지
        assertThat(roomReservationRepository.findById(id)).isPresent();
    }

    @Test
    void TC_ROOM_C_04_예약관계없는학생취소_403_NO_CANCEL_PERMISSION() throws Exception {
        long id = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0), 2);

        mockMvc.perform(delete("/api/meeting/reservations/{id}", id)
                        .param("studentId", String.valueOf(OUTSIDER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("NO_CANCEL_PERMISSION"));

        assertThat(roomReservationRepository.findById(id)).isPresent();
    }

    @Test
    void TC_ROOM_C_05_없는예약취소_404_RESERVATION_NOT_FOUND() throws Exception {
        mockMvc.perform(delete("/api/meeting/reservations/{id}", 999999L)
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESERVATION_NOT_FOUND"));
    }

    @Test
    void TC_ROOM_C_06_취소방식확인_삭제구현_재취소시_404() throws Exception {
        long id = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0), 2);

        // 1차 취소 성공
        mockMvc.perform(delete("/api/meeting/reservations/{id}", id)
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // 삭제 구현이므로 재취소는 "없는 예약"
        mockMvc.perform(delete("/api/meeting/reservations/{id}", id)
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESERVATION_NOT_FOUND"));
    }

    @Test
    void TC_ROOM_C_07_무효학번_400_INVALID_STUDENT_ID() throws Exception {
        long id = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0), 2);

        // 하드코딩된 무효 학번: 202099999 / 202288888
        mockMvc.perform(delete("/api/meeting/reservations/{id}", id)
                        .param("studentId", "202099999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_STUDENT_ID"));
    }
}
