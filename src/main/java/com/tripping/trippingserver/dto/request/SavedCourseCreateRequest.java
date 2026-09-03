package com.tripping.trippingserver.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "여행 코스 저장 요청")
public class SavedCourseCreateRequest {

    @NotBlank
    @Schema(
            description = "저장할 여행 코스 ID",
            example = "course-abc123"
    )
    private String courseId;
}
