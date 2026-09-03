package com.tripping.trippingserver.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "AI 메인 관광지 추천 요청")
public class RecommendationRequest {

    @NotBlank
    @Schema(
            description = "여행 유형",
            example = "자연",
            allowableValues = {"자연", "도시", "복합"}
    )
    private String travelType;

    @NotNull
    @Min(1)
    @Max(100)
    @Schema(
            description = "여행자의 연령",
            example = "20"
    )
    private Integer age;

    @NotBlank
    @Schema(
            description = "동행자 유형",
            example = "친구",
            allowableValues = {"혼자","친구","반려동물","부모님","아이","연인"}
    )
    private String companion;

    @Schema(
            description = "여행 지역. 선택 입력값입니다.",
            example = "경기도 수원"
    )
    private String region;

    @Schema(
            description = "추가 자연어 요구사항. 선택 입력값입니다.",
            example = "조용한 카페가 많고 산책하기 좋은 곳",
            nullable = true
    )
    private String requirement;
}
