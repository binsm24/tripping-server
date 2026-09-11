package com.tripping.trippingserver.external.groq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Getter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GroqApiClient {

    private final GroqApiProperties properties;
    private final ObjectMapper objectMapper;

    private final RestClient restClient =
            RestClient.builder().build();

    public Map<String, String> summarizePlaces(
            List<SummaryTarget> targets
    ) {
        if (targets == null || targets.isEmpty()) {
            return Map.of();
        }

        String targetJson;

        try {
            targetJson =
                    objectMapper.writeValueAsString(targets);

        } catch (Exception exception) {
            System.err.println(
                    "[Groq] 장소 목록 JSON 변환 실패: "
                            + exception.getMessage()
            );

            return Map.of();
        }

        String prompt = """
        다음 장소 목록의 원문 설명을 바탕으로
        각 장소마다 한국어 한 줄 summary를 작성해라.

        규칙:
        1. 입력된 모든 장소에 대해 summary를 하나씩 작성한다.
        2. placeId는 입력값을 그대로 유지한다.
        3. 원문에 있는 정보만 사용한다.
        4. 새로운 사실을 추가하지 않는다.
        5. summary는 자연스러운 한국어 한 문장으로 작성한다.
        6. summary는 30자에서 50자 사이로 작성한다.
        7. 장소명을 단순히 반복하지 않는다.
        8. <think>, </think>를 출력하지 않는다.
        9. 사고 과정이나 분석 과정을 출력하지 않는다.
        10. 마크다운 코드 블록을 사용하지 않는다.
        11. 반드시 JSON 객체 하나만 반환한다.
        12. 입력 장소 수와 동일한 개수의 summary를 반환한다.

        장소 목록:
        %s

        응답 형식:
        {
          "summaries": [
            {
              "placeId": "tourism-123",
              "summary": "장소의 한 줄 소개"
            }
          ]
        }
        """.formatted(targetJson);

        Map<String, Object> requestBody =
                Map.of(
                        "model", properties.getModel(),
                        "messages", List.of(
                                Map.of(
                                        "role", "user",
                                        "content", prompt
                                )
                        ),
                        "temperature", 0.2,
                        "max_tokens", 2500,
                        "reasoning_effort", "none",
                        "reasoning_format", "hidden"
                );

        try {
            String responseBody =
                    restClient.post()
                            .uri(
                                    properties.getBaseUrl()
                                            + "/chat/completions"
                            )
                            .header(
                                    "Authorization",
                                    "Bearer "
                                            + properties.getApiKey()
                            )
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(requestBody)
                            .retrieve()
                            .body(String.class);

            System.out.println(
                    "[Groq] batch raw response: "
                            + responseBody
            );

            JsonNode response =
                    objectMapper.readTree(responseBody);

            JsonNode contentNode =
                    response.path("choices")
                            .path(0)
                            .path("message")
                            .path("content");

            if (contentNode.isMissingNode()
                    || contentNode.isNull()
                    || contentNode.asText().isBlank()) {
                return Map.of();
            }

            String content =
                    cleanJsonResponse(
                            contentNode.asText()
                    );

            JsonNode summaryRoot =
                    objectMapper.readTree(content);

            JsonNode summaries =
                    summaryRoot.path("summaries");

            if (!summaries.isArray()) {
                return Map.of();
            }

            Map<String, String> result =
                    new HashMap<>();

            for (JsonNode summaryNode : summaries) {
                String placeId =
                        summaryNode.path("placeId").asText();

                String summary =
                        summaryNode.path("summary").asText();

                if (placeId == null
                        || placeId.isBlank()
                        || summary == null
                        || summary.isBlank()) {
                    continue;
                }

                result.put(
                        placeId,
                        summary.trim()
                );
            }

            return result;

        } catch (RestClientResponseException exception) {
            System.err.println(
                    "Groq HTTP status: "
                            + exception.getStatusCode()
            );

            System.err.println(
                    "Groq response body: "
                            + exception.getResponseBodyAsString()
            );

            exception.getResponseHeaders().forEach(
                    (name, values) ->
                            System.err.println(
                                    "Groq header - "
                                            + name
                                            + ": "
                                            + values
                            )
            );

            return Map.of();

        } catch (Exception exception) {
            System.err.println(
                    "[Groq] batch summary generation failed: "
                            + exception.getMessage()
            );

            return Map.of();
        }
    }

    private String cleanSummary(
            String content
    ) {
        if (content == null || content.isBlank()) {
            return null;
        }

        String cleaned = content
                .replaceAll("(?s)<think>.*?</think>", "")
                .trim();

        // 혹시 닫는 태그 없이 think가 시작된 경우 방어
        int thinkIndex = cleaned.indexOf("<think>");

        if (thinkIndex >= 0) {
            cleaned = cleaned
                    .substring(0, thinkIndex)
                    .trim();
        }

        // Qwen이 설명 문구를 추가할 경우 제거
        if (cleaned.startsWith("요약:")) {
            cleaned = cleaned
                    .substring("요약:".length())
                    .trim();
        }

        return cleaned.isBlank()
                ? null
                : cleaned;
    }

    private String cleanJsonResponse(
            String content
    ) {
        if (content == null || content.isBlank()) {
            return null;
        }

        String cleaned =
                content
                        .replaceAll(
                                "(?s)<think>.*?</think>",
                                ""
                        )
                        .replace(
                                "```json",
                                ""
                        )
                        .replace(
                                "```",
                                ""
                        )
                        .trim();

        int thinkIndex =
                cleaned.indexOf("<think>");

        if (thinkIndex >= 0) {
            cleaned =
                    cleaned.substring(
                            0,
                            thinkIndex
                    ).trim();
        }

        return cleaned;
    }

    @Getter
    public static class SummaryTarget {

        private final String placeId;
        private final String name;
        private final String sourceText;

        public SummaryTarget(
                String placeId,
                String name,
                String sourceText
        ) {
            this.placeId = placeId;
            this.name = name;
            this.sourceText = sourceText;
        }
    }
}