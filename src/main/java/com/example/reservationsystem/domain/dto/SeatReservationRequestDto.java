package com.example.reservationsystem.domain.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatReservationRequestDto {

    @NotNull(message = "좌석 번호를 입력해주세요.")
    @Min(value = 1, message = "좌석 번호는 1~70 사이여야 합니다.")
    @Max(value = 70, message = "좌석 번호는 1~70 사이여야 합니다.")
    private Long seatId;

    @NotBlank(message = "예약 날짜는 필수입니다. (YYYY-MM-DD)")
    private LocalDate date;

    @NotBlank(message = "시작 시간은 필수입니다. (HH:mm)")
    private LocalTime startTime;

    @NotNull(message = "이용 시간을 입력해주세요. (2 또는 4)")
    @Pattern(regexp = "2|4", message = "좌석 이용 시간은 2 또는 4시간만 선택할 수 있습니다.")
    private int durationHours;

    @NotNull(message = "학번은 필수 입력값입니다.")
    @Min(value = 100000000, message = "학번은 9자리 숫자여야 합니다.")
    @Max(value = 999999999, message = "학번은 9자리 숫자여야 합니다.")
    private Long studentId;

}