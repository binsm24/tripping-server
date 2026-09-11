package com.tripping.trippingserver.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

@Getter
@Builder
@Schema(description = "주변 장소 확장 추천 응답")
public class NearbyRecommendationResponse {

    @Schema(
            description = "기준 메인 관광지 ID",
            example = "place-001"
    )
    private String mainPlaceId;

    @Schema(description = "주변 관광지 목록")
    private List<NearbyPlace> attractions;

    @Schema(description = "주변 카페 목록")
    private List<NearbyPlace> cafes;

    @Schema(description = "주변 음식점 목록")
    private List<NearbyPlace> restaurants;

    @Getter
    @Builder
    @Schema(description = "주변 장소 정보")
    public static class NearbyPlace {

        @Schema(
                description = "장소 ID",
                example = "nearby-001"
        )
        private String placeId;

        @Schema(
                description = "장소명",
                example = "광교 앨리웨이"
        )
        private String name;

        @Schema(
                description = "대표 이미지 URL",
                example = "https://example.com/place.jpg"
        )
        private String imageUrl;

        @Schema(
                description = "관광공사 API의 장소 소개를 짧게 가공한 한 줄 설명",
                example = "메인 관광지 방문 전후에 들르기 좋은 장소입니다."
        )
        private String summary;

        @Schema(
                description = "장소 주소",
                example = "경기도 수원시 영통구 광교호수로 100"
        )
        private String address;

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

        @JsonIgnore
        private String sourceText;
    }
}
