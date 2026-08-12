package com.tripping.trippingserver.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "AI 메인 관광지 추천 응답")
public class RecommendationResponse {

    @Schema(
            description = "추천 결과 제목",
            example = "수원 자연 힐링 여행"
    )
    private String title;

    @Schema(
            description = "추천 세션 ID",
            example = "test-session-001"
    )
    private String recommendationSessionId;

    @Schema(description = "추천 관광지 목록")
    private List<RecommendedPlace> places;

    @Getter
    @Builder
    @Schema(description = "추천 관광지 정보")
    public static class RecommendedPlace {

        @Schema(
                description = "관광지 ID",
                example = "place-001"
        )
        private String placeId;

        @Schema(
                description = "관광지 이름",
                example = "광교호수공원"
        )
        private String name;

        @Schema(
                description = "대표 이미지 URL",
                example = "https://example.com/image.jpg"
        )
        private String imageUrl;

        @Schema(
                description = "AI 한 줄 소개",
                example = "도심 속에서 산책과 야경을 함께 즐길 수 있는 장소입니다."
        )
        private String summary;

        @Schema(
                description = "위도",
                example = "37.2851"
        )
        private Double latitude;

        @Schema(
                description = "경도",
                example = "127.0573"
        )
        private Double longitude;
    }
}
