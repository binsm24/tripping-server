package com.tripping.trippingserver.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "보관함 목록 항목")
public class SavedCourseSummaryResponse {

    @Schema(
            description = "보관함 항목 ID",
            example = "15"
    )
    private Long savedCourseId;

    @Schema(
            description = "원본 여행 코스 ID",
            example = "1"
    )
    private Long courseId;

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

    @Schema(
            description = "대표 지도 이미지 URL",
            example = "https://..."
    )
    private String mapImageUrl;

    @Schema(
            description = "코스 생성 또는 저장 시각",
            example = "2026-08-07T15:30:00"
    )
    private LocalDateTime createdAt;
}
