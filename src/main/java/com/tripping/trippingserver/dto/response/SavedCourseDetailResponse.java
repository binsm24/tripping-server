package com.tripping.trippingserver.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "보관함 상세 조회 응답")
public class SavedCourseDetailResponse {

    @Schema(
            description = "보관함 항목 ID",
            example = "saved-course-001"

    )
    private String savedCourseId;

    @Schema(
            description = "원본 여행 코스 ID",
            example = "course-abc123"
    )
    private String courseId;

    @Schema(
            description = "여행 코스명",
            example = "강릉 감성 힐링 여행"
    )
    private String courseTitle;

    @Schema(
            description = "예상 소요 시간",
            example = "약 6시간"
    )
    private String estimatedDuration;

    @Schema(description = "여행 코스 태그")
    private List<String> tags;

    @Schema(
            description = "여행 코스 소개",
            example = "푸른 바다와 감성 카페를 함께 즐길 수 있는 하루 코스입니다."
    )
    private String description;

    @Schema(
            description = "코스 지도 이미지 URL",
            example = "https://..."
    )
    private String mapImageUrl;

    @Schema(
            description = "코스 생성 또는 저장 시각",
            example = "2026-08-07T15:30:00"
    )
    private LocalDateTime createdAt;

    @Schema(description = "코스 방문 장소 목록")
    private List<CoursePlaceResponse> places;
}
