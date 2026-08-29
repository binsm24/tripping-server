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
                    자연어 요구사항을 기반으로 메인 관광지 3곳을 추천합니다.
                    현재는 Swagger 연동 확인을 위해 임시 데이터를 반환합니다.
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
            @ApiResponse(
                    responseCode = "502",
                    description = "관광공사 API 요청 실패"
            )
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
                선택한 메인 관광지와 추천 세션을 기반으로
                주변 관광지, 카페, 음식점을 추천합니다.
                현재는 Swagger 연동 확인을 위해 임시 데이터를 반환합니다.
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
            )
    })
    public ResponseEntity<NearbyRecommendationResponse> recommendNearby(
            @Valid @RequestBody NearbyRecommendationRequest request
    ) {
        NearbyRecommendationResponse response =
                recommendationService.recommendNearby(request);

        return ResponseEntity.ok(response);
    }
}
