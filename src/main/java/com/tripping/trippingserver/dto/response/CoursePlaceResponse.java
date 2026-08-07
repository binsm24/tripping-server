package com.tripping.trippingserver.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "여행 코스 방문 장소")
public class CoursePlaceResponse {

    @Schema(
            description = "방문 순서",
            example = "1"
    )
    private Integer order;

    @Schema(
            description = "장소 ID",
            example = "101"
    )
    private Long placeId;

    @Schema(
            description = "장소명",
            example = "안목해변"
    )
    private String name;

    @Schema(
            description = "장소 한 줄 설명",
            example = "푸른 바다를 바라보며 산책하기 좋은 해변입니다."
    )
    private String summary;

    @Schema(
            description = "장소 대표 이미지 URL",
            example = "https://..."
    )
    private String imageUrl;
}
