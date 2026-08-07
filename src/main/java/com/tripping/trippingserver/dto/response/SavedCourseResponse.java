package com.tripping.trippingserver.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "저장된 여행 코스 응답")
public class SavedCourseResponse {

    @Schema(
            description = "보관함 항목 ID",
            example = "saved-course-001"
    )
    private String savedCourseId;

    @Schema(
            description = "원본 여행 코스 ID",
            example = "course-001"
    )
    private String courseId;

    @Schema(
            description = "여행 코스명",
            example = "광교호수공원에서 즐기는 여유로운 하루"
    )
    private String title;

    @Schema(
            description = "여행 코스 대표 이미지 URL",
            example = "https://example.com/course-map.png"
    )
    private String imageUrl;

    @Schema(
            description = "여행 코스 소개",
            example = "호수 산책과 카페, 식사를 함께 즐길 수 있는 당일치기 코스입니다."
    )
    private String description;

    @Schema(description = "여행 코스 태그")
    private List<String> tags;
}
