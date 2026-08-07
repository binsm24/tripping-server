package com.tripping.trippingserver.service;

import com.tripping.trippingserver.dto.response.PlaceDetailResponse;
import org.springframework.stereotype.Service;

@Service
public class PlaceService {

    public PlaceDetailResponse getPlaceDetail(String placeId) {
        return PlaceDetailResponse.builder()
                .placeId(placeId)
                .name("광교호수공원")
                .description("도심 속에서 산책과 휴식을 함께 즐길 수 있는 관광지입니다.")
                .imageUrl("https://example.com/gwanggyo.jpg")
                .address("경기도 수원시 영통구 광교호수로 57")
                .phoneNumber("031-123-4567")
                .latitude(37.2851)
                .longitude(127.0573)
                .kakaoMapUrl("https://map.kakao.com/")
                .build();
    }
}
