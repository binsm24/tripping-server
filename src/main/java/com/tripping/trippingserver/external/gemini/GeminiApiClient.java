package com.tripping.trippingserver.external.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripping.trippingserver.exception.BusinessException;
import com.tripping.trippingserver.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestClientResponseException;


import java.net.URI;
import java.util.Map;
import java.util.List;

@Component
public class GeminiApiClient {

    private final GeminiApiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiApiClient(
            GeminiApiProperties properties,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    public String generateContent(String prompt) {
        System.out.println("Gemini model: " + properties.getModel());
        System.out.println("Gemini base URL: " + properties.getBaseUrl());
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(properties.getBaseUrl())
                    .path("/v1beta/models/")
                    .path(properties.getModel())
                    .path(":generateContent")
                    .queryParam("key", properties.getApiKey())
                    .build()
                    .toUri();

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", prompt)
                                    )
                            )
                    )
            );

            String rawResponse = restClient.post()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            System.out.println("Gemini response received");
            System.out.println(
                    "Gemini response length: "
                            + (rawResponse == null ? 0 : rawResponse.length())
            );

            return extractText(rawResponse);

        } catch (RestClientResponseException exception) {
            System.out.println("Gemini HTTP status: " + exception.getStatusCode());
            System.out.println("Gemini response body: "
                    + exception.getResponseBodyAsString());

            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "Gemini API 요청 중 오류가 발생했습니다."
            );

    } catch (BusinessException exception) {
            throw exception;

        } catch (Exception exception) {
            exception.printStackTrace();

            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "Gemini API 응답 처리 중 오류가 발생했습니다."
            );
        }
    }

    private String extractText(String rawResponse) {
        try {
            if (rawResponse == null || rawResponse.isBlank()) {
                throw new BusinessException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        "Gemini 응답이 비어 있습니다."
                );
            }

            JsonNode root =
                    objectMapper.readTree(rawResponse);

            if (root.has("error")) {
                String errorMessage =
                        root.path("error")
                                .path("message")
                                .asText("Gemini API 오류");

                throw new BusinessException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        errorMessage
                );
            }

            JsonNode candidates =
                    root.path("candidates");

            if (!candidates.isArray()
                    || candidates.isEmpty()) {
                throw new BusinessException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        "Gemini 응답에 candidates가 없습니다."
                );
            }

            JsonNode textNode = candidates
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            if (textNode.isMissingNode()
                    || textNode.asText().isBlank()) {
                throw new BusinessException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        "Gemini 응답에 생성된 텍스트가 없습니다."
                );
            }

            return textNode.asText();

        } catch (BusinessException exception) {
            throw exception;

        } catch (Exception exception) {
            exception.printStackTrace();

            throw new BusinessException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "Gemini 응답 JSON 파싱 중 오류가 발생했습니다."
            );
        }
    }
}
