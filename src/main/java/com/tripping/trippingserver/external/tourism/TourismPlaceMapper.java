package com.tripping.trippingserver.external.tourism;

import com.tripping.trippingserver.dto.response.NearbyRecommendationResponse;
import com.tripping.trippingserver.dto.response.PlaceDetailResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TourismPlaceMapper {

    public PlaceDetailResponse toPlaceDetailResponse(
            String contentId,
            TourismApiResponse apiResponse
    ) {

        if (apiResponse == null
                || apiResponse.getResponse() == null
                || apiResponse.getResponse().getBody() == null
                || apiResponse.getResponse().getBody().getItems() == null) {
            return null;
        }

        List<TourismApiResponse.Item> items =
                apiResponse.getResponse()
                        .getBody()
                        .getItems()
                        .getItem();

        if (items == null || items.isEmpty()) {
            return null;
        }

        TourismApiResponse.Item item = items.get(0);

        return PlaceDetailResponse.builder()
                .placeId("tourism-" + contentId)
                .name(item.getTitle())
                .description(item.getOverview())
                .imageUrl(resolveImageUrl(item))
                .address(makeAddress(item))
                .phoneNumber(item.getTel())
                .latitude(toDouble(item.getMapy()))
                .longitude(toDouble(item.getMapx()))
                .kakaoMapUrl("https://map.kakao.com/")
                .build();
    }

    public NearbyRecommendationResponse.NearbyPlace toNearbyPlace(
            TourismApiResponse.Item item
    ) {
        return NearbyRecommendationResponse.NearbyPlace.builder()
                .placeId("tourism-" + item.getContentid())
                .name(item.getTitle())
                .imageUrl(resolveImageUrl(item))
                .summary(resolveSummary(item))
                .address(makeAddress(item))
                .latitude(toDouble(item.getMapy()))
                .longitude(toDouble(item.getMapx()))
                .build();
    }

    private String resolveImageUrl(
            TourismApiResponse.Item item
    ) {
        String firstImage = item.getFirstimage();

        if (isValidImageUrl(firstImage)) {
            return firstImage;
        }

        String firstImage2 = item.getFirstimage2();

        if (isValidImageUrl(firstImage2)) {
            return firstImage2;
        }

        return null;
    }

    private boolean isValidImageUrl(
            String imageUrl
    ) {
        return imageUrl != null
                && !imageUrl.isBlank()
                && (
                imageUrl.startsWith("https://")
                        || imageUrl.startsWith("http://")
        );
    }

    private String resolveSummary(
            TourismApiResponse.Item item
    ) {
        if (item.getOverview() != null
                && !item.getOverview().isBlank()) {
            return item.getOverview().trim();
        }

        String title = item.getTitle();

        if (title == null || title.isBlank()) {
            return "주변 추천 장소입니다.";
        }

        return title + " 주변 추천 장소입니다.";
    }

    private String makeAddress(TourismApiResponse.Item item) {
        String addr1 = item.getAddr1();
        String addr2 = item.getAddr2();

        if (addr1 == null) {
            return addr2;
        }

        if (addr2 == null || addr2.isBlank()) {
            return addr1;
        }

        return addr1 + " " + addr2;
    }

    private Double toDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}