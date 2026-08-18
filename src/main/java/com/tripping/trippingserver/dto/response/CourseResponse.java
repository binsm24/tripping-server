package com.tripping.trippingserver.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "AI 여행 코스 생성 응답")
public class CourseResponse {

    @Schema(
            description = "여행 코스 ID",
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

    private List<String> tags;

    private String description;

    private String mapImageUrl;

    private List<CoursePlaceResponse> places;
}