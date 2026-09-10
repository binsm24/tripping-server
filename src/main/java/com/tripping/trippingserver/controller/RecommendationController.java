package com.tripping.trippingserver.controller;

import com.tripping.trippingserver.dto.request.RecommendationRequest;
import com.tripping.trippingserver.dto.response.RecommendationResponse;
import com.tripping.trippingserver.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.tripping.trippingserver.dto.request.NearbyRecommendationRequest;
import com.tripping.trippingserver.dto.response.NearbyRecommendationResponse;
import com.tripping.trippingserver.dto.request.CourseGenerationRequest;
import com.tripping.trippingserver.dto.response.CourseResponse;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(
        name = "Recommendation",
        description = "AI 관광지 추천 API"
)
public class RecommendationController {

    private final RecommendationService recommendationService;

    @PostMapping
    @Operation(
            summary = "AI 메인 관광지 추천",
            description = """
                    사용자의 여행 유형, 연령, 동행자, 지역,
                    자연어 요구사항을 기반으로 실제 관광공사 관광지 중 메인 관광지 3곳을 추천합니다.
                    Gemini가 여행 조건에 맞는 관광지를 선택하고 추천 이유를 생성합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "관광지 추천 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청"
            ),
    })
    public ResponseEntity<RecommendationResponse> recommend(
            @Valid @RequestBody RecommendationRequest request
    ) {
        RecommendationResponse response =
                recommendationService.recommend(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/nearby")
    @Operation(
            summary = "주변 관광지·카페·음식점 확장 추천",
            description = """
                선택한 메인 관광지를 기준으로 반경 5km 이내의
                관광지, 카페, 음식점을 관광공사 API에서 조회합니다.
                조회 결과는 관광지, 카페, 음식점 카테고리별로 제공합니다.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "주변 장소 추천 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "메인 관광지를 찾을 수 없음"
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "관광공사 API 요청 실패"
            )
    })
    public ResponseEntity<NearbyRecommendationResponse> recommendNearby(
            @Valid @RequestBody NearbyRecommendationRequest request
    ) {
        NearbyRecommendationResponse response =
                recommendationService.recommendNearby(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/course")
    @Operation(
            summary = "AI 여행 코스 생성",
            description = """
                선택한 메인 관광지와 주변 관광지·카페·음식점 목록,
                사용자의 여행 조건을 기반으로 Gemini가 하루 여행 코스를 생성합니다.
                코스의 첫 번째 장소는 선택한 메인 관광지로 고정됩니다. 
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "여행 코스 생성 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "주변 장소를 찾을 수 없음"
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "외부 API 요청 실패"
            )
    })
    public ResponseEntity<CourseResponse> generateCourse(
            @Valid @RequestBody CourseGenerationRequest request
    ) {
        CourseResponse response =
                recommendationService.generateCourse(
                        request
                );

        return ResponseEntity.ok(response);
    }
}
