package com.tripping.trippingserver.external.tourism;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Component
public class TourismApiClient {

    private final TourismApiProperties properties;
    private final RestClient restClient;

    public TourismApiClient(TourismApiProperties properties) {
        this.properties = properties;

        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    public TourismApiResponse getPlaceDetail(String contentId) {

        String url = properties.getBaseUrl()
                + "/detailCommon2"
                + "?serviceKey=" + properties.getApiKey()
                + "&MobileOS=ETC"
                + "&MobileApp=TripPing"
                + "&_type=json"
                + "&contentId=" + contentId;

        URI uri = URI.create(url);

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(TourismApiResponse.class);
    }

    public TourismApiResponse getNearbyPlaces(
            double longitude,
            double latitude,
            int radius
    ) {

        String url = properties.getBaseUrl()
                + "/locationBasedList2"
                + "?serviceKey=" + properties.getApiKey()
                + "&MobileOS=ETC"
                + "&MobileApp=TripPing"
                + "&_type=json"
                + "&mapX=" + longitude
                + "&mapY=" + latitude
                + "&radius=" + radius
                + "&numOfRows=30"
                + "&pageNo=1"
                + "&arrange=E";

        URI uri = URI.create(url);

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(TourismApiResponse.class);
    }
}