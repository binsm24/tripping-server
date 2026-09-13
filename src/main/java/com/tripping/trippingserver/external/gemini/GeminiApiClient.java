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
    private final java.time.Clock clock;
    private volatile java.time.Instant dailyQuotaResetAt;

    private static final java.time.ZoneId QUOTA_ZONE = java.time.ZoneId.of("America/Los_Angeles");
    private static final java.time.ZoneId DISPLAY_ZONE = java.time.ZoneId.of("Asia/Seoul");

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiApiClient(
            GeminiApiProperties properties,
            ObjectMapper objectMapper
    ) {
        this(properties, objectMapper, RestClient.builder(), java.time.Clock.systemUTC());
    }

    GeminiApiClient(GeminiApiProperties properties, ObjectMapper objectMapper,
                    RestClient.Builder builder, java.time.Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = builder.baseUrl(properties.getBaseUrl()).build();
        this.clock = clock;
    }

    public String generateContent(String prompt) {
        java.time.Instant resetAt = dailyQuotaResetAt;
        if (resetAt != null && clock.instant().isBefore(resetAt)) {
            throw dailyQuotaException(resetAt);
        }
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
            if (exception.getStatusCode().value() == 429
                    && isDailyQuotaExceeded(exception.getResponseBodyAsString())) {
                // Ignore short RetryInfo delays for daily quotas. Never sleep or retry this request.
                java.time.Instant nextReset = clock.instant().atZone(QUOTA_ZONE).toLocalDate()
                        .plusDays(1).atStartOfDay(QUOTA_ZONE).toInstant();
                dailyQuotaResetAt = nextReset;
                org.slf4j.LoggerFactory.getLogger(GeminiApiClient.class).warn(
                        "[Gemini] trace={} daily quota exhausted; automaticRetry=false resetAt={}",
                        org.slf4j.MDC.get("recommendationTrace"), nextReset);
                throw dailyQuotaException(nextReset);
            }
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

    private boolean isDailyQuotaExceeded(String body) {
        try {
            JsonNode details = objectMapper.readTree(body).path("error").path("details");
            if (!details.isArray()) return false;
            for (JsonNode detail : details) {
                if (!"type.googleapis.com/google.rpc.QuotaFailure".equals(detail.path("@type").asText())) continue;
                JsonNode violations = detail.path("violations");
                if (!violations.isArray()) continue;
                for (JsonNode violation : violations) {
                    if (violation.path("quotaId").asText("").toLowerCase(java.util.Locale.ROOT).contains("perday"))
                        return true;
                }
            }
        } catch (Exception ignored) {
            // Unknown responses retain the existing upstream-error handling.
        }
        return false;
    }

    private BusinessException dailyQuotaException(java.time.Instant resetAt) {
        String resetTime = java.time.format.DateTimeFormatter.ofPattern("M월 d일 HH:mm")
                .withZone(DISPLAY_ZONE).format(resetAt);
        return new BusinessException(ErrorCode.AI_DAILY_QUOTA_EXCEEDED,
                "AI 여행 추천 서비스의 일일 사용 한도가 소진되었습니다. 한국 시간 "
                        + resetTime + " 이후 다시 이용해 주세요.");
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
