package com.example.reservationsystem.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationLookupRequestDto {

    @NotBlank(message = "학번을 입력해주세요.")
    @Pattern(regexp = "^\\d{9}$", message = "학번은 9자리 숫자여야 합니다.")
    private Long studentId; // 조회할 학생 학번
}
