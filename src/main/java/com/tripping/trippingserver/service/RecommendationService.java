package com.tripping.trippingserver.service;

import com.tripping.trippingserver.dto.request.NearbyRecommendationRequest;
import com.tripping.trippingserver.dto.request.RecommendationRequest;
import com.tripping.trippingserver.dto.response.NearbyRecommendationResponse;
import com.tripping.trippingserver.dto.response.PlaceDetailResponse;
import com.tripping.trippingserver.dto.response.RecommendationResponse;
import com.tripping.trippingserver.external.tourism.TourismApiClient;
import com.tripping.trippingserver.external.tourism.TourismApiResponse;
import com.tripping.trippingserver.external.tourism.TourismPlaceMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecommendationService {

    private final TourismApiClient tourismApiClient;
    private final TourismPlaceMapper tourismPlaceMapper;

    public RecommendationService(
            TourismApiClient tourismApiClient,
            TourismPlaceMapper tourismPlaceMapper
    ) {
        this.tourismApiClient = tourismApiClient;
        this.tourismPlaceMapper = tourismPlaceMapper;
    }

    // 기존 추천 API
    // Gemini 단계 담당이므로 지금은 유지
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

    // 주변 추천 API
    public NearbyRecommendationResponse recommendNearby(
            NearbyRecommendationRequest request
    ) {

        // 1. 요청에서 메인 장소 ID 꺼내기
        String mainPlaceId = request.getMainPlaceId();

        // tourism-133854 -> 133854
        String contentId = mainPlaceId.replace("tourism-", "");

        // 2. 메인 장소 상세 조회
        TourismApiResponse detailResponse =
                tourismApiClient.getPlaceDetail(contentId);

        // 3. 관광공사 응답 -> 우리 DTO
        PlaceDetailResponse mainPlace =
                tourismPlaceMapper.toPlaceDetailResponse(
                        contentId,
                        detailResponse
                );

        // 상세 정보나 좌표가 없으면 주변 조회 불가
        if (mainPlace == null
                || mainPlace.getLatitude() == null
                || mainPlace.getLongitude() == null) {

            throw new IllegalArgumentException(
                    "메인 관광지를 찾을 수 없습니다."
            );
        }

        // 4. 메인 장소 기준 반경 3km 주변 조회
        TourismApiResponse nearbyResponse =
                tourismApiClient.getNearbyPlaces(
                        mainPlace.getLongitude(),
                        mainPlace.getLatitude(),
                        3000
                );

        // 5. 결과 담을 리스트
        List<NearbyRecommendationResponse.NearbyPlace> attractions =
                new ArrayList<>();

        List<NearbyRecommendationResponse.NearbyPlace> cafes =
                new ArrayList<>();

        List<NearbyRecommendationResponse.NearbyPlace> restaurants =
                new ArrayList<>();

        // 6. 주변 결과가 없으면 빈 리스트 반환
        if (nearbyResponse == null
                || nearbyResponse.getResponse() == null
                || nearbyResponse.getResponse().getBody() == null
                || nearbyResponse.getResponse().getBody().getItems() == null
                || nearbyResponse.getResponse()
                .getBody()
                .getItems()
                .getItem() == null) {

            return NearbyRecommendationResponse.builder()
                    .mainPlaceId(mainPlaceId)
                    .attractions(attractions)
                    .cafes(cafes)
                    .restaurants(restaurants)
                    .build();
        }

        // 7. 주변 장소 분류
        for (TourismApiResponse.Item item :
                nearbyResponse.getResponse()
                        .getBody()
                        .getItems()
                        .getItem()) {

            // 자기 자신 제외
            if (contentId.equals(item.getContentid())) {
                continue;
            }

            // 좌표 없는 장소 제외
            if (item.getMapx() == null
                    || item.getMapx().isBlank()
                    || item.getMapy() == null
                    || item.getMapy().isBlank()) {
                continue;
            }

            NearbyRecommendationResponse.NearbyPlace place =
                    tourismPlaceMapper.toNearbyPlace(item);

            String contentTypeId = item.getContenttypeid();

            // 39 = 음식점
            if ("39".equals(contentTypeId)) {

                if (restaurants.size() < 10) {
                    restaurants.add(place);
                }

            } else {

                if (attractions.size() < 10) {
                    attractions.add(place);
                }
            }

            // 둘 다 10개 찼으면 종료
            if (attractions.size() >= 10
                    && restaurants.size() >= 10) {
                break;
            }
        }

        // 8. 최종 응답
        return NearbyRecommendationResponse.builder()
                .mainPlaceId(mainPlaceId)
                .attractions(attractions)
                .cafes(cafes)
                .restaurants(restaurants)
                .build();
    }
}