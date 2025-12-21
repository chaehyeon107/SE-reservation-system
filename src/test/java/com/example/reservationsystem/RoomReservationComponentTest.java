package com.example.reservationsystem.component;

import com.example.reservationsystem.domain.entity.Room;
import com.example.reservationsystem.domain.entity.RoomReservation;
import com.example.reservationsystem.domain.entity.RoomReservationStatus;
import com.example.reservationsystem.domain.entity.Student;
import com.example.reservationsystem.domain.repository.RoomRepository;
import com.example.reservationsystem.domain.repository.RoomReservationParticipantRepository;
import com.example.reservationsystem.domain.repository.RoomReservationRepository;
import com.example.reservationsystem.domain.repository.StudentRepository;
import com.example.reservationsystem.testsupport.TcLogWatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;


import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ✅ Component Test (Controller + Service + Repository 연동)
 * - 대상: 회의실 예약 조회 / 취소
 * - 특징: API(MockMvc)로 요청하고, DB 상태(예약/학생 누적시간/취소 status)를 함께 검증
 *
 * ✅ TcLogWatcher 연동:
 * - 성공/실패 결과가 build/test-output.txt 에 누적 기록됨
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@ExtendWith(TcLogWatcher.class)
public class RoomReservationComponentTest {


    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired private WebApplicationContext wac;

    @Autowired RoomRepository roomRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired RoomReservationRepository roomReservationRepository;
    @Autowired RoomReservationParticipantRepository roomReservationParticipantRepository;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final long LEADER = 202212121L;
    private static final long P1 = 202111492L;
    private static final long P2 = 202213007L;
    private static final long OUTSIDER = 202299999L;

    private Long roomId;

    @BeforeEach
    void setUp() {
        this.mockMvc = webAppContextSetup(wac).build();
        // 매 테스트 독립성 확보 (H2 메모리에서도 “테스트 간 잔재” 방지)
        roomReservationParticipantRepository.deleteAll();
        roomReservationRepository.deleteAll();
        studentRepository.deleteAll();
        roomRepository.deleteAll();

        // Room 엔티티는 id 자동생성이라, 생성 후 id를 roomId로 사용
        Room savedRoom = roomRepository.save(Room.builder().capacity(3).build());
        roomId = savedRoom.getId();
    }

    // -----------------
    // Helpers
    // -----------------

    /**
     * 예약 생성 API를 실제로 호출하여 reservationId를 확보한다.
     * (Component Test 성격 유지: Controller -> Service -> Repo 경로를 그대로 태움)
     */
    private long createReservationViaApi(LocalDate date, LocalTime startTime, int durationHours) throws Exception {
        String body = objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
            put("roomId", roomId);
            put("date", date.toString());
            put("startTime", startTime.toString());
            put("duration", durationHours);
            put("representativeStudentId", LEADER);
            put("participantStudentIds", List.of(P1, P2));
        }});

        String res = mockMvc.perform(post("/api/meeting/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("회의실 예약이 생성되었습니다."))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(res);
        return root.path("payload").path("id").asLong();
    }

    private Student findStudent(long studentId) {
        return studentRepository.findByStudentId(studentId).orElseThrow();
    }

    // -----------------
    // LIST (by studentId)
    // -----------------

    @Test
    @DisplayName("[CT-ROOM-L-01] 내 예약 목록 조회(예약 존재) - studentId")
    void listByStudentId_hasReservations() throws Exception {
        long reservationId = createReservationViaApi(LocalDate.now(KST).plusDays(1), LocalTime.of(10, 0), 2);

        mockMvc.perform(get("/api/meeting/reservations")
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("회의실 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload[0].id").exists());

        // DB에도 실제로 존재 (취소 전)
        RoomReservation r = roomReservationRepository.findById(reservationId).orElseThrow();
        assertThat(r.getStatus()).isEqualTo(RoomReservationStatus.RESERVED);
    }

    @Test
    @DisplayName("[CT-ROOM-L-02] 내 예약 목록 조회(예약 없음) - studentId")
    void listByStudentId_empty() throws Exception {
        mockMvc.perform(get("/api/meeting/reservations")
                        .param("studentId", String.valueOf(OUTSIDER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("회의실 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload.length()").value(0));
    }

    @Test
    @DisplayName("[CT-ROOM-L-03] 조회 파라미터 누락 - INVALID_REQUEST")
    void list_invalidRequest() throws Exception {
        mockMvc.perform(get("/api/meeting/reservations"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    // -----------------
    // LIST (by date)
    // -----------------

    @Test
    @DisplayName("[CT-ROOM-L-04] 날짜 기반 회의실 일정 조회(date)")
    void listByDate_roomSchedule() throws Exception {
        LocalDate date = LocalDate.now(KST).plusDays(1);
        createReservationViaApi(date, LocalTime.of(11, 0), 2);

        mockMvc.perform(get("/api/meeting/reservations")
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("현재 예약 조회에 성공했습니다."))
                .andExpect(jsonPath("$.payload[0].reservations").isArray())
                .andExpect(jsonPath("$.payload[0].reservations[0].startTime").exists())
                .andExpect(jsonPath("$.payload[0].reservations[0].endTime").exists());
    }

    // -----------------
    // CANCEL
    // -----------------

    @Test
    @DisplayName("[CT-ROOM-C-01] 대표자 취소 성공(시작 전) - 환급 + status 변경")
    void cancel_refund_beforeStart() throws Exception {
        LocalDate date = LocalDate.now(KST).plusDays(1);
        long reservationId = createReservationViaApi(date, LocalTime.of(10, 0), 2);

        // 예약 생성 시점에 duration 만큼 사용시간이 누적되어 있어야 함
        Student leader = findStudent(LEADER);
        Student p1 = findStudent(P1);
        Student p2 = findStudent(P2);

        assertThat(leader.getMeetingDailyUsedHours()).isEqualTo(2);
        assertThat(p1.getMeetingDailyUsedHours()).isEqualTo(2);
        assertThat(p2.getMeetingDailyUsedHours()).isEqualTo(2);

        mockMvc.perform(patch("/api/meeting/reservations/{id}", reservationId)
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("예약이 정상적으로 취소되었습니다."))
                .andExpect(jsonPath("$.payload").doesNotExist());

        RoomReservation after = roomReservationRepository.findById(reservationId).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(RoomReservationStatus.CANCELED_REFUND);

        // 환급(-2) 적용: 0으로 복귀
        leader = findStudent(LEADER);
        p1 = findStudent(P1);
        p2 = findStudent(P2);

        assertThat(leader.getMeetingDailyUsedHours()).isEqualTo(0);
        assertThat(p1.getMeetingDailyUsedHours()).isEqualTo(0);
        assertThat(p2.getMeetingDailyUsedHours()).isEqualTo(0);
    }

    @Test
    @DisplayName("[CT-ROOM-C-02] 대표자 취소 성공(시작 후/종료 전) - 페널티(+duration) (운영시간 내 실행될 때만 검증)")
    void cancel_penalty_afterStart_beforeEnd() throws Exception {
        // ⚠️ 서비스가 LocalDateTime.now(KST)를 직접 사용하므로, 실행 시간이 운영시간에 들어오지 않으면 “안정적” 검증이 불가
        // -> 운영시간 경계(10:00~16:59)에만 수행하도록 assume 처리(그 외 시간은 SKIP 처리됨)
        LocalDateTime now = LocalDateTime.now(KST);
        LocalTime t = now.toLocalTime();

        Assumptions.assumeTrue(
                !t.isBefore(LocalTime.of(10, 0)) && t.isBefore(LocalTime.of(17, 0)),
                "현재 시간이 운영시간 경계(10:00~16:59) 밖이라 페널티 케이스를 스킵합니다."
        );

        // start <= now < end 되도록: start=now-1h, duration=2h
        LocalDate date = now.toLocalDate();
        LocalTime start = t.minusHours(1).withSecond(0).withNano(0);

        long reservationId = createReservationViaApi(date, start, 2);

        // 생성 시점 누적=2
        Student leader = findStudent(LEADER);
        assertThat(leader.getMeetingDailyUsedHours()).isEqualTo(2);

        mockMvc.perform(patch("/api/meeting/reservations/{id}", reservationId)
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("예약이 정상적으로 취소되었습니다."));

        RoomReservation after = roomReservationRepository.findById(reservationId).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(RoomReservationStatus.CANCELED_PENALTY);

        // 페널티(+2): meetingDailyUsedHours = 4
        leader = findStudent(LEADER);
        assertThat(leader.getMeetingDailyUsedHours()).isEqualTo(4);
    }

    @Test
    @DisplayName("[CT-ROOM-C-03] 비대표자 취소 차단 - NO_CANCEL_PERMISSION")
    void cancel_forbidden_notRepresentative() throws Exception {
        long reservationId = createReservationViaApi(LocalDate.now(KST).plusDays(1), LocalTime.of(10, 0), 2);

        mockMvc.perform(patch("/api/meeting/reservations/{id}", reservationId)
                        .param("studentId", String.valueOf(P1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("NO_CANCEL_PERMISSION"))
                .andExpect(jsonPath("$.message").value("예약 취소 권한은 대표자에게만 있습니다."));

        // DB status는 여전히 RESERVED
        RoomReservation after = roomReservationRepository.findById(reservationId).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(RoomReservationStatus.RESERVED);
    }

    @Test
    @DisplayName("[CT-ROOM-C-04] 예약과 무관한 학생 취소 차단 - NO_CANCEL_PERMISSION")
    void cancel_forbidden_outsider() throws Exception {
        long reservationId = createReservationViaApi(LocalDate.now(KST).plusDays(1), LocalTime.of(10, 0), 2);

        mockMvc.perform(patch("/api/meeting/reservations/{id}", reservationId)
                        .param("studentId", String.valueOf(OUTSIDER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("NO_CANCEL_PERMISSION"))
                .andExpect(jsonPath("$.message").value("예약 취소 권한은 대표자에게만 있습니다."));
    }

    @Test
    @DisplayName("[CT-ROOM-C-05] 취소 대상 없음 - RESERVATION_NOT_FOUND")
    void cancel_notFound() throws Exception {
        mockMvc.perform(patch("/api/meeting/reservations/{id}", 999999)
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESERVATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("현재 등록된 회의실 예약이 없습니다."));
    }

    @Test
    @DisplayName("[CT-ROOM-C-06] 이미 종료된 예약 취소 차단 - RESERVATION_ALREADY_FINISHED")
    void cancel_conflict_alreadyFinished() throws Exception {
        // 어제 09:00~11:00 => now(after end) 보장
        long reservationId = createReservationViaApi(LocalDate.now(KST).minusDays(1), LocalTime.of(9, 0), 2);

        mockMvc.perform(patch("/api/meeting/reservations/{id}", reservationId)
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESERVATION_ALREADY_FINISHED"))
                .andExpect(jsonPath("$.message").value("이미 종료된 예약은 취소할 수 없습니다."));

        RoomReservation after = roomReservationRepository.findById(reservationId).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(RoomReservationStatus.RESERVED);
    }

    @Test
    @DisplayName("[CT-ROOM-C-07] 이미 취소된 예약 재취소 차단 - ALREADY_CANCELED_RESERVATION")
    void cancel_conflict_alreadyCanceled() throws Exception {
        long reservationId = createReservationViaApi(LocalDate.now(KST).plusDays(1), LocalTime.of(10, 0), 2);

        // 1회 취소
        mockMvc.perform(patch("/api/meeting/reservations/{id}", reservationId)
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isOk());

        // 2회 취소
        mockMvc.perform(patch("/api/meeting/reservations/{id}", reservationId)
                        .param("studentId", String.valueOf(LEADER)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("ALREADY_CANCELED_RESERVATION"))
                .andExpect(jsonPath("$.message").value("이미 취소된 예약입니다."));
    }
}
