package com.tripping.trippingserver.controller;

import com.tripping.trippingserver.dto.request.SavedCourseCreateRequest;
import com.tripping.trippingserver.dto.response.ApiResponse;
import com.tripping.trippingserver.dto.response.SavedCourseDetailResponse;
import com.tripping.trippingserver.dto.response.SavedCourseSummaryResponse;
import com.tripping.trippingserver.service.SavedCourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/saved-courses")
@RequiredArgsConstructor
@Tag(
        name = "Saved Course",
        description = "저장된 여행 코스 및 보관함 API"
)
public class SavedCourseController {

    private final SavedCourseService savedCourseService;

    @PostMapping
    @Operation(
            summary = "여행 코스 저장",
            description = "생성된 여행 코스를 사용자의 보관함에 저장합니다."
    )
    public ResponseEntity<ApiResponse<SavedCourseDetailResponse>> saveCourse(
            @RequestParam String userId,
            @Valid @RequestBody SavedCourseCreateRequest request
    ) {
        SavedCourseDetailResponse data =
                savedCourseService.saveCourse(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "saved course created successfully",
                        data
                )
        );
    }

    @GetMapping
    @Operation(
            summary = "보관함 목록 조회",
            description = "사용자가 저장한 여행 코스 목록을 조회합니다."
    )
    public ResponseEntity<ApiResponse<List<SavedCourseSummaryResponse>>> getSavedCourses(
            @RequestParam String userId
    ) {
        List<SavedCourseSummaryResponse> data =
                savedCourseService.getSavedCourses(userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "saved courses retrieved successfully",
                        data
                )
        );
    }

    @GetMapping("/{savedCourseId}")
    @Operation(
            summary = "보관함 상세 조회",
            description = "저장된 여행 코스의 상세 정보를 조회합니다."
    )
    public ResponseEntity<ApiResponse<SavedCourseDetailResponse>> getSavedCourse(
            @Parameter(
                    description = "보관함 항목 ID",
                    example = "15"
            )
            @PathVariable String savedCourseId
    ) {
        SavedCourseDetailResponse data =
                savedCourseService.getSavedCourse(savedCourseId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "saved course retrieved successfully",
                        data
                )
        );
    }
}
