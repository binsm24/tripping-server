package com.tripping.trippingserver.service;

import com.tripping.trippingserver.dto.request.RecommendationRequest;
import com.tripping.trippingserver.dto.response.RecommendationResponse;
import org.springframework.stereotype.Service;
import com.tripping.trippingserver.dto.request.NearbyRecommendationRequest;
import com.tripping.trippingserver.dto.response.NearbyRecommendationResponse;

import java.util.List;

@Service
public class RecommendationService {

    public RecommendationResponse recommend(
            RecommendationRequest request
    ) {
        return RecommendationResponse.builder()
                .title(request.getRegion() + " 맞춤 여행 추천")
                .recommendationSessionId("test-session-001")
                .places(List.of(
                        RecommendationResponse.RecommendedPlace.builder()
                                .placeId("place-001")
                                .name("광교호수공원")
                                .imageUrl("https://example.com/gwanggyo.jpg")
                                .summary("도심 속에서 산책과 야경을 함께 즐길 수 있는 장소입니다.")
                                .latitude(37.2851)
                                .longitude(127.0573)
                                .build(),

                        RecommendationResponse.RecommendedPlace.builder()
                                .placeId("place-002")
                                .name("화성행궁")
                                .imageUrl("https://example.com/hwaseong.jpg")
                                .summary("역사와 산책을 함께 즐길 수 있는 대표 관광지입니다.")
                                .latitude(37.2819)
                                .longitude(127.0148)
                                .build(),

                        RecommendationResponse.RecommendedPlace.builder()
                                .placeId("place-003")
                                .name("방화수류정")
                                .imageUrl("https://example.com/banghwasuryujeong.jpg")
                                .summary("수원화성의 경관과 여유로운 휴식을 즐길 수 있습니다.")
                                .latitude(37.2876)
                                .longitude(127.0171)
                                .build()
                ))
                .build();
    }
    public NearbyRecommendationResponse recommendNearby(
            NearbyRecommendationRequest request
    ) {
        return NearbyRecommendationResponse.builder()
                .mainPlaceId(request.getMainPlaceId())
                .attractions(List.of(
                        NearbyRecommendationResponse.NearbyPlace.builder()
                                .placeId("nearby-attraction-001")
                                .name("광교 앨리웨이")
                                .imageUrl("https://example.com/alleyway.jpg")
                                .summary("호수공원과 함께 방문하기 좋은 복합 문화 공간입니다.")
                                .address("경기도 수원시 영통구 광교호수로 100")
                                .latitude(37.2897)
                                .longitude(127.0558)
                                .build()
                ))
                .cafes(List.of(
                        NearbyRecommendationResponse.NearbyPlace.builder()
                                .placeId("nearby-cafe-001")
                                .name("광교 카페거리")
                                .imageUrl("https://example.com/cafe.jpg")
                                .summary("산책 후 여유롭게 쉬어가기 좋은 카페입니다.")
                                .address("경기도 수원시 영통구 광교중앙로 150")
                                .latitude(37.2931)
                                .longitude(127.0567)
                                .build()
                ))
                .restaurants(List.of(
                        NearbyRecommendationResponse.NearbyPlace.builder()
                                .placeId("nearby-restaurant-001")
                                .name("광교 호수 근처 맛집")
                                .imageUrl("https://example.com/restaurant.jpg")
                                .summary("여행 중 식사하기 좋은 주변 음식점입니다.")
                                .address("경기도 수원시 영통구 센트럴타운로 20")
                                .latitude(37.2908)
                                .longitude(127.0519)
                                .build()
                ))
                .build();
    }
}
