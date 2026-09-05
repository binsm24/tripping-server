package com.tripping.trippingserver.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "AI 여행 코스 생성 요청")
public class CourseGenerationRequest {

    @NotBlank
    @Schema(
            description = "메인 관광지 ID",
            example = "tourism-800630"
    )
    private String mainPlaceId;

    @NotBlank
    @Schema(
            description = "메인 추천 세션 ID",
            example = "test-session-001"
    )
    private String recommendationSessionId;

    @NotBlank
    @Schema(
            description = "여행 지역",
            example = "양평"
    )
    private String region;

    @NotBlank
    @Schema(
            description = "여행 유형",
            example = "자연"
    )
    private String travelType;

    @Schema(description = "여행자 나이", example = "20")
    private Integer age;

    @Schema(description = "동행자", example = "친구")
    private String companion;

    @Schema(
            description = "추가 요구사항",
            example = "조용히 산책하고 카페도 가고 싶어요"
    )
    private String requirement;

    @NotNull
    @Size(min = 1)
    @Schema(
            description = "사용자가 코스에 추가하기로 선택한 주변 장소 ID 목록",
            example = "[\"tourism-123\", \"tourism-456\", \"tourism-789\"]"
    )
    private List<String> selectedPlaceIds;
}