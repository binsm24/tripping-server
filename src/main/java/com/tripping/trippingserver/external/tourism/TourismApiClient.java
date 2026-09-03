package com.tripping.trippingserver.external.tourism;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.tripping.trippingserver.exception.BusinessException;
import com.tripping.trippingserver.exception.ErrorCode;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;

@Component
public class TourismApiClient {

    private final TourismApiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public TourismApiClient(
            TourismApiProperties properties
    ) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();

        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    public TourismApiResponse getPlaceDetail(String contentId) {

        try {
            String url = properties.getBaseUrl()
                    + "/detailCommon2"
                    + "?serviceKey=" + properties.getApiKey()
                    + "&MobileOS=ETC"
                    + "&MobileApp=TripPing"
                    + "&_type=json"
                    + "&contentId=" + contentId;

            URI uri = URI.create(url);

            String rawResponse = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);


            if (rawResponse == null || rawResponse.isBlank()) {
                return null;
            }

            if (rawResponse.contains("\"items\": \"\"")
                    || rawResponse.contains("\"items\":\"\"")) {
                return null;
            }

            return objectMapper.readValue(
                    rawResponse,
                    TourismApiResponse.class
            );

        } catch (JsonProcessingException e) {

            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR
            );

        } catch (RestClientException e) {

            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR
            );
        }
    }

    public TourismApiResponse getNearbyPlaces(
            double longitude,
            double latitude,
            int radius
    ) {

        try {
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

        } catch (RestClientException e) {

            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR
            );
        }
    }
}