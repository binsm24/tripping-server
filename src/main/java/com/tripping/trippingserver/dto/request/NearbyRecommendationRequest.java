package com.tripping.trippingserver.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "주변 장소 확장 추천 요청")
public class NearbyRecommendationRequest {

    @NotBlank
    @Schema(
            description = "메인 관광지 ID",
            example = "place-001"
    )
    private String mainPlaceId;

    @NotBlank
    @Schema(
            description = "메인 관광지 추천 세션 ID",
            example = "test-session-001"
    )
    private String recommendationSessionId;
}
