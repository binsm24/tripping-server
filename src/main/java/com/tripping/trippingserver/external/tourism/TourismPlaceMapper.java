package com.tripping.trippingserver.external.tourism;

import com.tripping.trippingserver.dto.response.NearbyRecommendationResponse;
import com.tripping.trippingserver.dto.response.PlaceDetailResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TourismPlaceMapper {

    public PlaceDetailResponse toPlaceDetailResponse(
            String contentId,
            TourismApiResponse commonResponse,
            TourismApiResponse introResponse,
            TourismApiResponse infoResponse
    ) {

        // 1. 기본 상세정보 확인
        if (commonResponse == null
                || commonResponse.getResponse() == null
                || commonResponse.getResponse().getBody() == null
                || commonResponse.getResponse().getBody().getItems() == null) {
            return null;
        }

        List<TourismApiResponse.Item> items =
                commonResponse.getResponse()
                        .getBody()
                        .getItems()
                        .getItem();

        if (items == null || items.isEmpty()) {
            return null;
        }

        TourismApiResponse.Item item = items.get(0);

        // 2. detailIntro2의 첫 번째 항목
        TourismApiResponse.Item introItem =
                getFirstItem(introResponse);

        // 3. detailInfo2에서 "입장료" 찾기
        String admissionFee =
                findInfoValue(
                        infoResponse,
                        "입장료"
                );

        // 4. 세 API의 정보를 하나의 응답 DTO로 합침
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

                // detailIntro2
                .openingHours(
                        introItem != null
                                ? introItem.getUsetime()
                                : null
                )
                .restDate(
                        introItem != null
                                ? introItem.getRestdate()
                                : null
                )
                .parking(
                        introItem != null
                                ? introItem.getParking()
                                : null
                )

                // detailInfo2
                .admissionFee(admissionFee)

                .build();
    }

    public NearbyRecommendationResponse.NearbyPlace toNearbyPlace(
            TourismApiResponse.Item item
    ) {
        return NearbyRecommendationResponse.NearbyPlace.builder()
                .placeId("tourism-" + item.getContentid())
                .name(item.getTitle())
                .imageUrl(resolveImageUrl(item))
                .summary(
                        summarize(
                                item.getTitle(),
                                item.getOverview()
                        )
                )
                .address(makeAddress(item))
                .latitude(toDouble(item.getMapy()))
                .longitude(toDouble(item.getMapx()))
                .build();
    }

    public String summarize(
            String title,
            String description
    ) {
        if (description == null
                || description.isBlank()) {
            return title == null || title.isBlank()
                    ? null
                    : title;
        }

        String summary = description
                .replaceAll("(?s)<think>.*?</think>", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (summary.isBlank()) {
            return title;
        }

        int maxLength = 50;

        if (summary.length() <= maxLength) {
            return summary;
        }

        return summary.substring(0, maxLength).trim() + "...";
    }

    private String resolveSummary(
            String title,
            String description
    ) {
        if (description == null
                || description.isBlank()) {
            return null;
        }

        String summary = description
                .replaceAll("\\s+", " ")
                .trim();

        int maxLength = 50;

        if (summary.length() <= maxLength) {
            return summary;
        }

        return summary.substring(0, maxLength).trim() + "...";
    }

    // detailIntro2 응답의 첫 번째 item 꺼내기
    private TourismApiResponse.Item getFirstItem(
            TourismApiResponse response
    ) {

        if (response == null
                || response.getResponse() == null
                || response.getResponse().getBody() == null
                || response.getResponse().getBody().getItems() == null
                || response.getResponse()
                .getBody()
                .getItems()
                .getItem() == null
                || response.getResponse()
                .getBody()
                .getItems()
                .getItem()
                .isEmpty()) {

            return null;
        }

        return response.getResponse()
                .getBody()
                .getItems()
                .getItem()
                .get(0);
    }

    // detailInfo2에서 원하는 항목 찾기
    // 예: infoname = "입장료"
    private String findInfoValue(
            TourismApiResponse response,
            String infoName
    ) {

        if (response == null
                || response.getResponse() == null
                || response.getResponse().getBody() == null
                || response.getResponse().getBody().getItems() == null
                || response.getResponse()
                .getBody()
                .getItems()
                .getItem() == null) {

            return null;
        }

        for (TourismApiResponse.Item item :
                response.getResponse()
                        .getBody()
                        .getItems()
                        .getItem()) {

            if (infoName.equals(item.getInfoname())) {
                return item.getInfotext();
            }
        }

        return null;
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

    // 관광공사 API에서 주는 설명인 summary는 30자로 제한
    private String resolveSummary(
            TourismApiResponse.Item item
    ) {
        String title = item.getTitle();
        String overview = item.getOverview();

        if (overview == null || overview.isBlank()) {
            if (title == null || title.isBlank()) {
                return "추천 장소입니다.";
            }

            return title;
        }

        String summary = overview
                .replaceAll("\\s+", " ")
                .trim();

        int maxLength = 30;

        if (summary.length() <= maxLength) {
            return summary;
        }

        return summary.substring(0, maxLength).trim() + "...";
    }

    private String makeAddress(
            TourismApiResponse.Item item
    ) {

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