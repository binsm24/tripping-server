package com.tripping.trippingserver.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "관광지 상세 조회 응답")
public class PlaceDetailResponse {

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
            description = "관광지 상세 설명",
            example = "광교호수공원은 도심 속에서 산책과 휴식을 즐길 수 있는 관광지입니다."
    )
    private String description;

    @Schema(
            description = "대표 이미지 URL",
            example = "https://example.com/gwanggyo.jpg"
    )
    private String imageUrl;

    @Schema(
            description = "주소",
            example = "경기도 수원시 영통구 광교호수로 57"
    )
    private String address;

    @Schema(
            description = "전화번호",
            example = "031-123-4567"
    )
    private String phoneNumber;

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

    @Schema(
            description = "카카오맵 장소 URL",
            example = "https://map.kakao.com/"
    )
    private String kakaoMapUrl;

    @Schema(
            description = "개방시간",
            example = "상시 개방"
    )
    private String openingHours;

    @Schema(
            description = "휴무일",
            example = "연중무휴"
    )
    private String restDate;

    @Schema(
            description = "주차정보",
            example = "가능"
    )
    private String parking;

    @Schema(
            description = "입장료",
            example = "무료"
    )
    private String admissionFee;
}
