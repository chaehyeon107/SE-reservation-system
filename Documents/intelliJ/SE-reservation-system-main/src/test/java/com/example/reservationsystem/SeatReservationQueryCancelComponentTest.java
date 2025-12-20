package com.example.reservationsystem;

import com.example.reservationsystem.domain.entity.Seat;
import com.example.reservationsystem.domain.entity.SeatReservation;
import com.example.reservationsystem.domain.repository.SeatRepository;
import com.example.reservationsystem.domain.repository.SeatReservationRepository;
import com.example.reservationsystem.domain.repository.StudentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 노트북 열람실 좌석 예약 조회 및 취소 컴포넌트 테스트
 * 요구사항 명세서 3.1.2.3, 3.1.2.4 기반
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SeatReservationQueryCancelComponentTest {

    private MockMvc mockMvc;
    @Autowired private WebApplicationContext wac;
    @Autowired ObjectMapper objectMapper;

    @Autowired SeatRepository seatRepository;
    @Autowired SeatReservationRepository seatReservationRepository;
    @Autowired StudentRepository studentRepository;

    private static final long STUDENT_1 = 202111492L;
    private static final long STUDENT_2 = 202213007L;

    @BeforeEach
    void setup() {
        this.mockMvc = webAppContextSetup(wac).build();

        seatReservationRepository.deleteAll();
        seatReservationRepository.flush();
        studentRepository.deleteAll();
        studentRepository.flush();

        if (seatRepository.count() == 0) {
            IntStream.rangeClosed(1, 70).forEach(i ->
                    seatRepository.save(Seat.builder().seatNumber(i).status("AVAILABLE").build())
            );
        }

        assertThat(seatRepository.count()).isGreaterThanOrEqualTo(70);
    }

    private long anySeatPk() {
        List<Seat> seats = seatRepository.findAll();
        assertThat(seats).isNotEmpty();
        return seats.get(0).getId();
    }

    /**
     * 좌석 예약 생성 헬퍼 메서드
     */
    private long createSeatReservation(long studentId, long seatPk, LocalDate date, LocalTime start, int hours) throws Exception {
        String body = """
            {
              "studentId": %d,
              "seatId": %d,
              "date": "%s",
              "startTime": "%s",
              "durationHours": %d
            }
            """.formatted(studentId, seatPk, date, start, hours);

        String res = mockMvc.perform(post("/api/seats/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("좌석 예약이 완료되었습니다."))
                .andExpect(jsonPath("$.payload.id").exists())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(res);
        return root.path("payload").path("id").asLong();
    }

    // ===================================================================
    // 3.1.2.4. 노트북 열람실 좌석 예약 조회 테스트
    // ===================================================================
    @Nested
    @DisplayName("노트북 열람실 좌석 예약 조회 테스트 (3.1.2.4)")
    class SeatReservationQueryTest {

        @Test
        @DisplayName("TC_SEAT_Q_01: 좌석 예약 조회 성공 - 예약이 1건 있는 경우")
        void TC_SEAT_Q_01_좌석예약조회_성공_예약있음() throws Exception {
            // Given: 예약 1건 생성
            LocalDate date = LocalDate.now().plusDays(1);
            long seatPk = anySeatPk();
            createSeatReservation(STUDENT_1, seatPk, date, LocalTime.of(9, 0), 2);

            // When & Then: 예약 조회
            mockMvc.perform(get("/api/seats/reservations")
                            .param("studentId", String.valueOf(STUDENT_1)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.message").value("내 좌석 예약 조회에 성공했습니다."))
                    .andExpect(jsonPath("$.payload").isArray())
                    .andExpect(jsonPath("$.payload.length()").value(1));
        }

        @Test
        @DisplayName("TC_SEAT_Q_02: 좌석 예약 조회 성공 - 예약이 없는 경우 빈 리스트 반환")
        void TC_SEAT_Q_02_좌석예약조회_성공_예약없음() throws Exception {
            // Given: 예약 없음

            // When & Then: 예약 조회 - 빈 리스트 반환
            mockMvc.perform(get("/api/seats/reservations")
                            .param("studentId", String.valueOf(STUDENT_1)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.message").value("내 좌석 예약 조회에 성공했습니다."))
                    .andExpect(jsonPath("$.payload").isArray())
                    .andExpect(jsonPath("$.payload.length()").value(0));
        }

        @Test
        @DisplayName("TC_SEAT_Q_03: 좌석 예약 조회 성공 - 여러 건의 예약이 있는 경우")
        void TC_SEAT_Q_03_좌석예약조회_성공_여러건() throws Exception {
            // Given: 예약 2건 생성 (같은 날 다른 시간대)
            LocalDate date = LocalDate.now().plusDays(1);
            long seatPk1 = anySeatPk();
            List<Seat> seats = seatRepository.findAll();
            long seatPk2 = seats.get(1).getId();

            createSeatReservation(STUDENT_1, seatPk1, date, LocalTime.of(9, 0), 2);
            createSeatReservation(STUDENT_1, seatPk2, date, LocalTime.of(11, 0), 2);

            // When & Then: 예약 조회 - 2건 반환
            mockMvc.perform(get("/api/seats/reservations")
                            .param("studentId", String.valueOf(STUDENT_1)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.message").value("내 좌석 예약 조회에 성공했습니다."))
                    .andExpect(jsonPath("$.payload").isArray())
                    .andExpect(jsonPath("$.payload.length()").value(2));
        }

        @Test
        @DisplayName("TC_SEAT_Q_04: 좌석 예약 조회 - 다른 학생의 예약은 조회되지 않음")
        void TC_SEAT_Q_04_좌석예약조회_본인예약만조회() throws Exception {
            // Given: STUDENT_1 예약 1건, STUDENT_2 예약 1건
            LocalDate date = LocalDate.now().plusDays(1);
            long seatPk1 = anySeatPk();
            List<Seat> seats = seatRepository.findAll();
            long seatPk2 = seats.get(1).getId();

            createSeatReservation(STUDENT_1, seatPk1, date, LocalTime.of(9, 0), 2);
            createSeatReservation(STUDENT_2, seatPk2, date, LocalTime.of(9, 0), 2);

            // When & Then: STUDENT_1 조회 시 본인 예약만 반환
            mockMvc.perform(get("/api/seats/reservations")
                            .param("studentId", String.valueOf(STUDENT_1)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.payload.length()").value(1));
        }

        @Test
        @DisplayName("TC_SEAT_Q_05: 좌석 예약 조회 실패 - 무효 학번 (INVALID_STUDENT_ID)")
        void TC_SEAT_Q_05_좌석예약조회_실패_무효학번() throws Exception {
            // Given: 무효 학번 (202099999 또는 202288888)
            long invalidStudentId = 202099999L;

            // When & Then: 무효 학번으로 조회 시도 - INVALID_STUDENT_ID 에러
            mockMvc.perform(get("/api/seats/reservations")
                            .param("studentId", String.valueOf(invalidStudentId)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_STUDENT_ID"));
        }
    }

    // ===================================================================
    // 3.1.2.3. 노트북 열람실 좌석 예약 취소 테스트
    // ===================================================================
    @Nested
    @DisplayName("노트북 열람실 좌석 예약 취소 테스트 (3.1.2.3)")
    class SeatReservationCancelTest {

        @Test
        @DisplayName("TC_SEAT_C_01: 좌석 예약 취소 성공 - 예약 시작 전 (시간 환급)")
        void TC_SEAT_C_01_좌석예약취소_성공_시간환급() throws Exception {
            // Given: 미래 날짜로 예약 생성
            LocalDate date = LocalDate.now().plusDays(1);
            long seatPk = anySeatPk();
            long reservationId = createSeatReservation(STUDENT_1, seatPk, date, LocalTime.of(9, 0), 2);

            // 예약이 존재하는지 확인
            assertThat(seatReservationRepository.findById(reservationId)).isPresent();

            // When & Then: 예약 취소
            mockMvc.perform(delete("/api/seats/reservations/{id}", reservationId)
                            .param("studentId", String.valueOf(STUDENT_1)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.message").value("좌석 예약이 취소되었습니다."))
                    .andExpect(jsonPath("$.payload").value("시간 환급"));

            // 예약이 삭제되었는지 확인
            assertThat(seatReservationRepository.findById(reservationId)).isEmpty();
        }

        @Test
        @DisplayName("TC_SEAT_C_02: 좌석 예약 취소 실패 - 존재하지 않는 예약")
        void TC_SEAT_C_02_좌석예약취소_실패_예약없음() throws Exception {
            // Given: 존재하지 않는 예약 ID
            long nonExistentReservationId = 99999L;

            // When & Then: 예약 취소 시도 - 실패
            mockMvc.perform(delete("/api/seats/reservations/{id}", nonExistentReservationId)
                            .param("studentId", String.valueOf(STUDENT_1)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.errorCode").value("SEAT_RESERVATION_NOT_FOUND1"));
        }

        @Test
        @DisplayName("TC_SEAT_C_03: 좌석 예약 취소 실패 - 다른 사람의 예약 취소 시도 (권한 없음)")
        void TC_SEAT_C_03_좌석예약취소_실패_권한없음() throws Exception {
            // Given: STUDENT_1의 예약 생성
            LocalDate date = LocalDate.now().plusDays(1);
            long seatPk = anySeatPk();
            long reservationId = createSeatReservation(STUDENT_1, seatPk, date, LocalTime.of(9, 0), 2);

            // When & Then: STUDENT_2가 취소 시도 - 권한 없음 에러
            mockMvc.perform(delete("/api/seats/reservations/{id}", reservationId)
                            .param("studentId", String.valueOf(STUDENT_2)))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.errorCode").value("NO_CANCEL_PERMISSION1"));

            // 예약이 여전히 존재하는지 확인
            assertThat(seatReservationRepository.findById(reservationId)).isPresent();
        }

        @Test
        @DisplayName("TC_SEAT_C_04: 좌석 예약 취소 후 좌석 예약 가능 상태 확인")
        void TC_SEAT_C_04_좌석예약취소_후_좌석예약가능() throws Exception {
            // Given: 예약 생성 후 취소
            LocalDate date = LocalDate.now().plusDays(1);
            long seatPk = anySeatPk();
            long reservationId = createSeatReservation(STUDENT_1, seatPk, date, LocalTime.of(9, 0), 2);

            // 예약 취소
            mockMvc.perform(delete("/api/seats/reservations/{id}", reservationId)
                            .param("studentId", String.valueOf(STUDENT_1)))
                    .andExpect(status().isOk());

            // When & Then: 같은 좌석, 같은 시간대로 다시 예약 가능
            long newReservationId = createSeatReservation(STUDENT_2, seatPk, date, LocalTime.of(9, 0), 2);
            assertThat(seatReservationRepository.findById(newReservationId)).isPresent();
        }

        @Test
        @DisplayName("TC_SEAT_C_05: 좌석 예약 취소 후 예약 조회 시 취소된 예약 미표시")
        void TC_SEAT_C_05_좌석예약취소_후_조회시_미표시() throws Exception {
            // Given: 예약 2건 생성
            LocalDate date = LocalDate.now().plusDays(1);
            long seatPk1 = anySeatPk();
            List<Seat> seats = seatRepository.findAll();
            long seatPk2 = seats.get(1).getId();

            long reservationId1 = createSeatReservation(STUDENT_1, seatPk1, date, LocalTime.of(9, 0), 2);
            createSeatReservation(STUDENT_1, seatPk2, date, LocalTime.of(11, 0), 2);

            // 1건 취소
            mockMvc.perform(delete("/api/seats/reservations/{id}", reservationId1)
                            .param("studentId", String.valueOf(STUDENT_1)))
                    .andExpect(status().isOk());

            // When & Then: 조회 시 1건만 반환
            mockMvc.perform(get("/api/seats/reservations")
                            .param("studentId", String.valueOf(STUDENT_1)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload.length()").value(1));
        }

        @Test
        @DisplayName("TC_SEAT_C_06: 좌석 예약 취소 시 일일 사용 시간 환급 확인")
        void TC_SEAT_C_06_좌석예약취소_일일사용시간환급() throws Exception {
            // Given: 2시간 예약 생성 (일일 한도: 4시간)
            LocalDate date = LocalDate.now().plusDays(1);
            long seatPk1 = anySeatPk();
            List<Seat> seats = seatRepository.findAll();
            long seatPk2 = seats.get(1).getId();
            long seatPk3 = seats.get(2).getId();

            long reservationId1 = createSeatReservation(STUDENT_1, seatPk1, date, LocalTime.of(9, 0), 2);
            createSeatReservation(STUDENT_1, seatPk2, date, LocalTime.of(11, 0), 2);

            // 이 시점에서 4시간 사용 완료 -> 추가 예약 불가

            // 첫 번째 예약 취소 (2시간 환급)
            mockMvc.perform(delete("/api/seats/reservations/{id}", reservationId1)
                            .param("studentId", String.valueOf(STUDENT_1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload").value("시간 환급"));

            // When & Then: 환급 후 2시간 추가 예약 가능
            createSeatReservation(STUDENT_1, seatPk3, date, LocalTime.of(13, 0), 2);

            // 조회 시 2건 존재
            mockMvc.perform(get("/api/seats/reservations")
                            .param("studentId", String.valueOf(STUDENT_1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload.length()").value(2));
        }

        @Test
        @DisplayName("TC_SEAT_C_07: 좌석 예약 취소 실패 - 이미 이용 시작된 예약 (SEAT_ALREADY_IN_USE)")
        void TC_SEAT_C_07_좌석예약취소_실패_이미시작된예약() throws Exception {
            // Given: 과거 날짜로 예약 데이터 직접 생성 (이미 시작된 예약)
            LocalDate pastDate = LocalDate.now().minusDays(1);
            long seatPk = anySeatPk();

            // Student 생성
            com.example.reservationsystem.domain.entity.Student student =
                    studentRepository.findByStudentId(STUDENT_1)
                            .orElseGet(() -> studentRepository.save(
                                    com.example.reservationsystem.domain.entity.Student.of(STUDENT_1)));

            // 과거 날짜의 예약 직접 생성
            Seat seat = seatRepository.findById(seatPk).orElseThrow();
            SeatReservation pastReservation = seatReservationRepository.save(
                    SeatReservation.of(seat, student, pastDate, LocalTime.of(9, 0), 2)
            );
            long reservationId = pastReservation.getId();

            // When & Then: 이미 시작된 예약 취소 시도 - SEAT_ALREADY_IN_USE 에러
            mockMvc.perform(delete("/api/seats/reservations/{id}", reservationId)
                            .param("studentId", String.valueOf(STUDENT_1)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.errorCode").value("SEAT_ALREADY_IN_USE"));

            // 예약이 삭제되지 않고 여전히 존재하는지 확인
            assertThat(seatReservationRepository.findById(reservationId)).isPresent();
        }

        @Test
        @DisplayName("TC_SEAT_C_08: 좌석 예약 취소 실패 - 무효 학번 (INVALID_STUDENT_ID)")
        void TC_SEAT_C_08_좌석예약취소_실패_무효학번() throws Exception {
            // Given: 예약 생성 후 무효 학번으로 취소 시도
            LocalDate date = LocalDate.now().plusDays(1);
            long seatPk = anySeatPk();
            long reservationId = createSeatReservation(STUDENT_1, seatPk, date, LocalTime.of(9, 0), 2);

            long invalidStudentId = 202099999L;

            // When & Then: 무효 학번으로 취소 시도 - INVALID_STUDENT_ID 에러
            mockMvc.perform(delete("/api/seats/reservations/{id}", reservationId)
                            .param("studentId", String.valueOf(invalidStudentId)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_STUDENT_ID"));

            // 예약이 삭제되지 않고 여전히 존재하는지 확인
            assertThat(seatReservationRepository.findById(reservationId)).isPresent();
        }

        @Test
        @DisplayName("TC_SEAT_C_09: 날짜 경계값 - 다른 날짜 예약 취소 시 해당 날짜 기준으로 한도 리셋")
        void TC_SEAT_C_09_날짜경계값_다른날짜_예약취소_한도리셋() throws Exception {
            // Given: 서로 다른 날짜에 예약 생성
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            LocalDate dayAfterTomorrow = LocalDate.now().plusDays(2);
            List<Seat> seats = seatRepository.findAll();
            long seatPk1 = seats.get(0).getId();
            long seatPk2 = seats.get(1).getId();
            long seatPk3 = seats.get(2).getId();

            // 내일 예약 2건 (4시간 - 일일 한도 도달)
            createSeatReservation(STUDENT_1, seatPk1, tomorrow, LocalTime.of(9, 0), 2);
            createSeatReservation(STUDENT_1, seatPk2, tomorrow, LocalTime.of(11, 0), 2);

            // 모레 예약 1건 (2시간)
            long dayAfterReservationId = createSeatReservation(STUDENT_1, seatPk3, dayAfterTomorrow, LocalTime.of(9, 0), 2);

            // When: 모레 예약 취소
            mockMvc.perform(delete("/api/seats/reservations/{id}", dayAfterReservationId)
                            .param("studentId", String.valueOf(STUDENT_1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload").value("시간 환급"));

            // Then: 내일 예약 2건은 그대로 유지
            mockMvc.perform(get("/api/seats/reservations")
                            .param("studentId", String.valueOf(STUDENT_1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload.length()").value(2));
        }

        @Test
        @DisplayName("TC_SEAT_C_10: 날짜 경계값 - 같은 날짜 예약 취소 후 환급된 시간으로 재예약 가능")
        void TC_SEAT_C_10_날짜경계값_같은날짜_취소후_재예약() throws Exception {
            // Given: 같은 날 일일 한도(4시간) 모두 사용
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            List<Seat> seats = seatRepository.findAll();
            long seatPk1 = seats.get(0).getId();
            long seatPk2 = seats.get(1).getId();
            long seatPk3 = seats.get(2).getId();

            long reservationId1 = createSeatReservation(STUDENT_1, seatPk1, tomorrow, LocalTime.of(9, 0), 2);
            createSeatReservation(STUDENT_1, seatPk2, tomorrow, LocalTime.of(11, 0), 2);

            // 일일 한도 초과로 추가 예약 불가 확인
            String body = """
                {
                  "studentId": %d,
                  "seatId": %d,
                  "date": "%s",
                  "startTime": "14:00",
                  "durationHours": 2
                }
                """.formatted(STUDENT_1, seatPk3, tomorrow);

            mockMvc.perform(post("/api/seats/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("SEAT_DAILY_LIMIT_EXCEEDED"));

            // When: 첫 번째 예약 취소 (2시간 환급)
            mockMvc.perform(delete("/api/seats/reservations/{id}", reservationId1)
                            .param("studentId", String.valueOf(STUDENT_1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload").value("시간 환급"));

            // Then: 환급 후 같은 날 2시간 추가 예약 가능
            mockMvc.perform(post("/api/seats/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));

            // 최종 예약 2건 확인
            mockMvc.perform(get("/api/seats/reservations")
                            .param("studentId", String.valueOf(STUDENT_1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload.length()").value(2));
        }

        @Test
        @DisplayName("TC_SEAT_C_11: 날짜 경계값 - 날짜 변경 시 일일 한도 독립적으로 관리됨")
        void TC_SEAT_C_11_날짜경계값_일일한도_독립관리() throws Exception {
            // Given: 내일 일일 한도(4시간) 모두 사용
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            LocalDate dayAfterTomorrow = LocalDate.now().plusDays(2);
            List<Seat> seats = seatRepository.findAll();
            long seatPk1 = seats.get(0).getId();
            long seatPk2 = seats.get(1).getId();
            long seatPk3 = seats.get(2).getId();

            createSeatReservation(STUDENT_1, seatPk1, tomorrow, LocalTime.of(9, 0), 2);
            createSeatReservation(STUDENT_1, seatPk2, tomorrow, LocalTime.of(11, 0), 2);

            // When: 모레는 별도의 일일 한도 적용 - 예약 가능
            String bodyDayAfter = """
                {
                  "studentId": %d,
                  "seatId": %d,
                  "date": "%s",
                  "startTime": "09:00",
                  "durationHours": 2
                }
                """.formatted(STUDENT_1, seatPk3, dayAfterTomorrow);

            mockMvc.perform(post("/api/seats/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyDayAfter))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));

            // Then: 총 3건 예약 확인 (내일 2건 + 모레 1건)
            mockMvc.perform(get("/api/seats/reservations")
                            .param("studentId", String.valueOf(STUDENT_1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload.length()").value(3));
        }
    }
}
