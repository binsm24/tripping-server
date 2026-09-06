package com.tripping.trippingserver.controller;

import com.tripping.trippingserver.dto.request.CourseGenerationRequest;
import com.tripping.trippingserver.dto.response.ApiResponse;
import com.tripping.trippingserver.dto.response.CourseResponse;
import com.tripping.trippingserver.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(
        name = "Course",
        description = "AI 여행 코스 생성 API"
)
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @Operation(
            summary = "AI 여행 코스 생성",
            description = """
                    사용자가 선택한 메인 관광지와 주변 장소를 기반으로
                    Gemini를 이용해 실제 여행 코스를 생성합니다.
                    """
    )
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @Valid @RequestBody CourseGenerationRequest request
    ) {
        CourseResponse data =
                courseService.createCourse(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "course created successfully",
                        data
                )
        );
    }
}