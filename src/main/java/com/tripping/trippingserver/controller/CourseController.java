package com.tripping.trippingserver.controller;

import com.tripping.trippingserver.dto.request.CourseCreateRequest;
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
                    사용자가 선택한 장소들을 기반으로
                    여행 코스명, 예상 소요 시간, 태그, 소개,
                    방문 순서와 지도 이미지 URL을 생성합니다.
                    현재는 테스트용 임시 데이터를 반환합니다.
                    """
    )
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @Valid @RequestBody CourseCreateRequest request
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
