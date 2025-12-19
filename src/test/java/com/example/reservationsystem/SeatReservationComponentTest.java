package com.example.reservationsystem;

import com.example.reservationsystem.domain.entity.Seat;
import com.example.reservationsystem.domain.entity.SeatReservation;
import com.example.reservationsystem.domain.repository.SeatRepository;
import com.example.reservationsystem.domain.repository.SeatReservationRepository;
import com.example.reservationsystem.domain.repository.StudentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SeatReservationComponentTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired private WebApplicationContext wac;

    @Autowired SeatRepository seatRepository;
    @Autowired SeatReservationRepository seatReservationRepository;
    @Autowired StudentRepository studentRepository;

    private static final long S1 = 202111492L;

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
        assertThat(seatRepository.findById(1L)).isPresent();
        assertThat(seatRepository.findById(70L)).isPresent();
    }

    private long anySeatPk() {
        List<Seat> seats = seatRepository.findAll();
        assertThat(seats).isNotEmpty();
        return seats.get(0).getId();
    }

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
                .andExpect(jsonPath("$.message").value("좌석 예약 완료"))
                .andExpect(jsonPath("$.payload.id").exists())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(res);
        return root.path("payload").path("id").asLong();
    }

    @Test
    void TC_SEAT_S_01_좌석지정예약_성공_2시간() throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);
        long seatPk = anySeatPk();

        long reservationId = createSeatReservation(S1, seatPk, date, LocalTime.of(9, 0), 2);

        SeatReservation saved = seatReservationRepository.findById(reservationId).orElseThrow();
        assertThat(saved.getSeat().getId()).isEqualTo(seatPk);

        mockMvc.perform(get("/api/seats/reservations")
                        .param("studentId", String.valueOf(S1)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("내 좌석 예약 조회 성공"))
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload.length()").value(1));
    }

    @Test
    void TC_SEAT_S_03_좌석지정예약_실패_운영시간위반() throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);
        long seatPk = anySeatPk();

        mockMvc.perform(post("/api/seats/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "studentId": %d,
                              "seatId": %d,
                              "date": "%s",
                              "startTime": "08:00",
                              "durationHours": 2
                            }
                            """.formatted(S1, seatPk, date)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("OUT_OF_OPERATING_HOURS"));
    }

    @Test
    void TC_SEAT_S_06_좌석지정예약_실패_좌석중복() throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);
        long seatPk = anySeatPk();

        createSeatReservation(S1, seatPk, date, LocalTime.of(9, 0), 2);

        mockMvc.perform(post("/api/seats/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "studentId": %d,
                              "seatId": %d,
                              "date": "%s",
                              "startTime": "09:00",
                              "durationHours": 2
                            }
                            """.formatted(202213007L, seatPk, date)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("SEAT_ALREADY_RESERVED"));
    }
}
