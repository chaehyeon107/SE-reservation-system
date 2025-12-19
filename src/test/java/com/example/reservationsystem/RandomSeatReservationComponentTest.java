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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;


@ActiveProfiles("test")
@SpringBootTest
class RandomSeatReservationComponentTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired SeatRepository seatRepository;
    @Autowired private WebApplicationContext wac;
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

        assertThat(seatRepository.findById(1L)).isPresent();
        assertThat(seatRepository.findById(70L)).isPresent();
    }

    private long seatPkAtIndex(int index) {
        this.mockMvc = webAppContextSetup(wac).build();
        List<Seat> seats = seatRepository.findAll();
        assertThat(seats.size()).isGreaterThan(index);
        return seats.get(index).getId();
    }

    private long createFixedReservation(long studentId, long seatPk, LocalDate date, LocalTime start, int hours) throws Exception {
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
                .andExpect(jsonPath("$.payload.id").exists())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(res);
        return root.path("payload").path("id").asLong();
    }

    @Test
    void TC_SEAT_R_01_랜덤예약_성공() throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);

        mockMvc.perform(post("/api/seats/reservations/random")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "studentId": %d,
                              "date": "%s",
                              "startTime": "09:00",
                              "durationHours": 2
                            }
                            """.formatted(S1, date)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message").value("랜덤 좌석 예약 완료"))
                .andExpect(jsonPath("$.payload.id").exists());
    }

    @Test
    void TC_SEAT_R_03_랜덤예약_예약된좌석_제외해야함() throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);

        Set<Long> reservedSeatPks = new HashSet<>();
        reservedSeatPks.add(seatPkAtIndex(0));
        reservedSeatPks.add(seatPkAtIndex(1));
        reservedSeatPks.add(seatPkAtIndex(2));

        for (long seatPk : reservedSeatPks) {
            createFixedReservation(202111492L + seatPk, seatPk, date, LocalTime.of(9, 0), 2);
        }

        String res = mockMvc.perform(post("/api/seats/reservations/random")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "studentId": %d,
                              "date": "%s",
                              "startTime": "09:00",
                              "durationHours": 2
                            }
                            """.formatted(S1, date)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.payload.id").exists())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(res);
        long reservationId = root.path("payload").path("id").asLong();

        SeatReservation saved = seatReservationRepository.findById(reservationId).orElseThrow();
        long pickedSeatPk = saved.getSeat().getId();

        assertThat(reservedSeatPks.contains(pickedSeatPk)).isFalse();
    }

    @Test
    void TC_SEAT_R_02_랜덤예약_실패_가능좌석없음_409() throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);

        List<Long> seatPks = seatRepository.findAll().stream().map(Seat::getId).toList();
        assertThat(seatPks.size()).isGreaterThanOrEqualTo(70);

        for (int i = 0; i < 70; i++) {
            long seatPk = seatPks.get(i);
            long studentId = 300000000L + i;

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
                        """.formatted(studentId, seatPk, date)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/seats/reservations/random")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "studentId": %d,
                      "date": "%s",
                      "startTime": "09:00",
                      "durationHours": 2
                    }
                    """.formatted(202111492L, date)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.errorCode").value("NO_AVAILABLE_SEATS"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
