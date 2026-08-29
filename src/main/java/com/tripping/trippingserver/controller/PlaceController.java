package com.tripping.trippingserver.controller;

import com.tripping.trippingserver.dto.response.PlaceDetailResponse;
import com.tripping.trippingserver.service.PlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
@Tag(
        name = "Place",
        description = "관광지 정보 조회 API"
)
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping("/{placeId}")
    @Operation(
            summary = "관광지 상세 조회",
            description = "관광지 ID를 기준으로 관광지 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "관광지 상세 조회 성공"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "관광지를 찾을 수 없음"
            )
    })
    public ResponseEntity<PlaceDetailResponse> getPlaceDetail(
            @Parameter(
                    description = "조회할 관광지 ID",
                    example = "place-001"
            )
            @PathVariable String placeId
    ) {
        PlaceDetailResponse response =
                placeService.getPlaceDetail(placeId);

        return ResponseEntity.ok(response);
    }
}