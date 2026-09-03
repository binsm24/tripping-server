package com.tripping.trippingserver.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "AI 여행 코스 생성 요청")
public class CourseCreateRequest {

    @NotBlank
    @Schema(
            description = "메인 관광지 ID",
            example = "place-001"
    )
    private String mainPlaceId;

    @NotEmpty
    @Schema(
            description = "사용자가 선택한 장소 ID 목록",
            example = "[\"place-001\", \"nearby-cafe-001\", \"nearby-restaurant-001\"]"
    )
    private List<@NotBlank String> selectedPlaceIds;
}
