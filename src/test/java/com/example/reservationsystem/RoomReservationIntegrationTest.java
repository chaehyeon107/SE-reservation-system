package com.example.reservationsystem;

import com.example.reservationsystem.domain.entity.Room;
import com.example.reservationsystem.domain.entity.Student;
import com.example.reservationsystem.domain.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ✅ 회의실 예약 "조회(내 예약)" + "취소" 통합테스트
 * - 프론트 없이 MockMvc로 API 호출
 * - H2(DB) 기반으로 Repository 상태/Student 사용시간까지 검증
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK) // ✅ 권장
class RoomReservationIntegrationTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired private WebApplicationContext wac;

    @Autowired SeatReservationRepository seatReservationRepository;
    @Autowired SeatRepository seatRepository;
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
        // ✅ AutoConfigureMockMvc 없이도 MockMvc 사용 가능
        this.mockMvc = webAppContextSetup(wac).build();

        // FK 순서 고려해서 정리
        seatReservationRepository.deleteAll();
        roomReservationParticipantRepository.deleteAll();
        roomReservationRepository.deleteAll();
        studentRepository.deleteAll();
        seatRepository.deleteAll();
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
    /* 오류 없는 버전
    @Test
    void TC_ROOM_L_03_취소후목록반영() throws Exception {
        long id = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0), 2);

        // 취소(PATCH)
        mockMvc.perform(patch("/api/meeting/reservations/{id}", id)
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // 조회 후 반영 검증:
        // 1) 목록에서 사라지거나
        // 2) 목록에 남아있다면 "취소 상태"여야 한다
        mockMvc.perform(get("/api/meeting/reservations")
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(result -> {
                    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
                    JsonNode payload = root.path("payload");

                    boolean found = false;
                    for (JsonNode item : payload) {
                        if (item.path("id").asLong() == id) {
                            found = true;

                            // ✅ status 필드가 있으면 취소 상태인지 확인
                            // (백엔드 응답에 status가 없다면 이 부분은 아래 "대안"으로)
                            String status = item.path("status").asText(null);
                            assertThat(status)
                                    .as("취소된 예약이 목록에 남아있다면 status가 취소 상태여야 합니다.")
                                    .isIn("CANCELED_REFUND", "CANCELED_PENALTY");
                        }
                    }

                    // found가 false면(목록에서 제외)도 OK
                });
    }
*/

    /*
    백엔드 구현이 ‘취소=삭제’가 아니라 ‘취소=상태 변경’이고,
    조회 API가 ‘취소 예약도 포함해서 반환’하는 구현 일거임(추측)
    그래서 위 취소후목록반영에 취소된 내역도 포함해서 에러가 발생
     */
    @Test
    void TC_ROOM_L_03_취소후목록반영() throws Exception {
        long id = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0), 2);

        // ✅ 컨트롤러는 DELETE가 아니라 PATCH
        mockMvc.perform(patch("/api/meeting/reservations/{id}", id)
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // ✅ 서비스 구현은 "삭제"가 아니라 "상태 변경"이므로
        //    목록에서 안 나오거나(리포지토리 쿼리에서 제외), 취소 상태로 표시될 수 있음
        // => 여기서는 최소 보장: "같은 id가 payload에 그대로 남아있지 않아야 한다" 정도로만 체크
        mockMvc.perform(get("/api/meeting/reservations")
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
                    for (JsonNode item : root.path("payload")) {
                        assertThat(item.path("id").asLong()).isNotEqualTo(id);
                    }
                });
    }

    // ----------------------------------------------------------------------
    // S-ROOM-CANCEL
    // ----------------------------------------------------------------------

    @Test
    void TC_ROOM_C_01_대표자취소_시작전_환급() throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);
        long id = createReservation(date, LocalTime.of(10, 0), 2);

        // 생성 시점: 참가자 전원 +2
        assertThat(mustFindStudent(LEADER).getMeetingDailyUsedHours()).isEqualTo(2);
        assertThat(mustFindStudent(P1).getMeetingDailyUsedHours()).isEqualTo(2);
        assertThat(mustFindStudent(P2).getMeetingDailyUsedHours()).isEqualTo(2);

        mockMvc.perform(patch("/api/meeting/reservations/{id}", id)
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // 시작 전 취소 -> 환급(-2) => 0
        assertThat(mustFindStudent(LEADER).getMeetingDailyUsedHours()).isEqualTo(0);
        assertThat(mustFindStudent(P1).getMeetingDailyUsedHours()).isEqualTo(0);
        assertThat(mustFindStudent(P2).getMeetingDailyUsedHours()).isEqualTo(0);
    }

    @Test
    void TC_ROOM_C_02_대표자취소_시작후_페널티() throws Exception {
        LocalDate date = LocalDate.now().minusDays(1);
        long id = createReservation(date, LocalTime.of(10, 0), 2);

        assertThat(mustFindStudent(LEADER).getMeetingDailyUsedHours()).isEqualTo(2);

        mockMvc.perform(patch("/api/meeting/reservations/{id}", id)
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // 시작 후 취소 -> 페널티(+2) => create(+2)+penalty(+2)=4
        assertThat(mustFindStudent(LEADER).getMeetingDailyUsedHours()).isEqualTo(4);
        assertThat(mustFindStudent(P1).getMeetingDailyUsedHours()).isEqualTo(4);
        assertThat(mustFindStudent(P2).getMeetingDailyUsedHours()).isEqualTo(4);
    }

    @Test
    void TC_ROOM_C_03_비대표자취소_403() throws Exception {
        long id = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0), 2);

        mockMvc.perform(patch("/api/meeting/reservations/{id}", id)
                        .param("studentId", String.valueOf(P1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("NO_CANCEL_PERMISSION"));
    }

    @Test
    void TC_ROOM_C_04_예약관계없는학생취소_403() throws Exception {
        long id = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0), 2);

        mockMvc.perform(patch("/api/meeting/reservations/{id}", id)
                        .param("studentId", String.valueOf(OUTSIDER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("NO_CANCEL_PERMISSION"));
    }

    @Test
    void TC_ROOM_C_05_없는예약취소_404() throws Exception {
        mockMvc.perform(patch("/api/meeting/reservations/{id}", 999999L)
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESERVATION_NOT_FOUND"));
    }

    @Test
    void TC_ROOM_C_06_재취소_4xx() throws Exception {
        long id = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0), 2);

        mockMvc.perform(patch("/api/meeting/reservations/{id}", id)
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        // 서비스 구현상 ALREADY_CANCELED_RESERVATION 으로 떨어질 가능성이 높음
        mockMvc.perform(patch("/api/meeting/reservations/{id}", id)
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.isSuccess").value(false));
    }

    @Test
    void TC_ROOM_C_07_무효학번_400() throws Exception {
        long id = createReservation(LocalDate.now().plusDays(1), LocalTime.of(10, 0), 2);

        mockMvc.perform(patch("/api/meeting/reservations/{id}", id)
                        .param("studentId", "202099999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_STUDENT_ID"));
    }
}
