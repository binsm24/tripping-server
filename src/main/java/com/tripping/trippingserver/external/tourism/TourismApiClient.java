package com.tripping.trippingserver.external.tourism;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.tripping.trippingserver.exception.BusinessException;
import com.tripping.trippingserver.exception.ErrorCode;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

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

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory();

        requestFactory.setReadTimeout(
                Duration.ofSeconds(30)
        );
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
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
            e.printStackTrace();

            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "관광공사 주변 장소 API 응답 시간이 초과되었습니다."
            );
        }
    }

    public TourismApiResponse searchPlacesByKeyword(
            String keyword
    ) {
        try {
            String encodedKeyword =
                    URLEncoder.encode(
                            keyword,
                            StandardCharsets.UTF_8
                    );

            String url = properties.getBaseUrl()
                    + "/searchKeyword2"
                    + "?serviceKey=" + properties.getApiKey()
                    + "&MobileOS=ETC"
                    + "&MobileApp=TripPing"
                    + "&_type=json"
                    + "&keyword=" + encodedKeyword
                    + "&contentTypeId=12"
                    + "&numOfRows=30"
                    + "&pageNo=1"
                    + "&arrange=Q";

            URI uri = URI.create(url);

            String rawResponse = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            System.out.println("Tourism keyword: " + keyword);
            System.out.println("Tourism response: " + rawResponse);

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

        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "관광지 검색 응답을 파싱할 수 없습니다."
            );

        } catch (RestClientException exception) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "관광지 검색 API 요청에 실패했습니다."
            );
        }
    }
}