package com.tripping.trippingserver.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "여행 코스 방문 장소")
public class CoursePlaceResponse {

    private Integer order;

    @Schema(
            description = "장소 ID",
            example = "tourism-101"
    )
    private String placeId;

    private String name;
    private String summary;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
}