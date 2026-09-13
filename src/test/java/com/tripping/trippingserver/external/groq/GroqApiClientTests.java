package com.tripping.trippingserver.external.groq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GroqApiClientTests {
    private final ObjectMapper json = new ObjectMapper();
    private final AtomicLong now = new AtomicLong(100_000);
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final GroqApiClient client = new GroqApiClient(
            new GroqApiProperties("test-key", "https://example.test", "qwen/qwen3.6-27b"), json, builder, now::get);

    private List<GroqApiClient.SummaryTarget> targets(int start, int count) {
        return IntStream.range(start, start + count)
                .mapToObj(i -> new GroqApiClient.SummaryTarget("tourism-" + i, "장소" + i, "원문 설명" + i)).toList();
    }
    private String response(int start, int count, int used) throws Exception {
        String content = json.writeValueAsString(Map.of("summaries", IntStream.range(start, start + count)
                .mapToObj(i -> Map.of("placeId", "tourism-" + i, "summary", "자연을 즐기며 산책하기 좋은 곳")).toList()));
        return json.writeValueAsString(Map.of("usage", Map.of("completion_tokens", used),
                "choices", List.of(Map.of("finish_reason", "stop", "message", Map.of("content", content)))));
    }
    private void expectBatch(int start, int count, int used) throws Exception {
        server.expect(requestTo("https://example.test/chat/completions"))
                .andExpect(content().string(containsString("\"max_tokens\":600")))
                .andRespond(withSuccess(response(start, count, used), MediaType.APPLICATION_JSON));
    }

    @Test
    void mainUses600TokensAndRepeatedSourceUsesCache() throws Exception {
        expectBatch(0, 3, 120);
        assertEquals(3, client.summarizePlaces(targets(0, 3)).size());
        assertEquals(3, client.summarizePlaces(targets(0, 3)).size());
        server.verify();
    }

    @Test
    void nearbyIsSplitAndSuccessfulBatchesAreMerged() throws Exception {
        expectBatch(0, 3, 120);
        expectBatch(3, 3, 120);
        assertEquals(6, client.summarizePlaces(targets(0, 6)).size());
        server.verify();
    }

    @Test
    void rateLimitStopsFollowingBatchesAndRequestsWithoutSleeping() throws Exception {
        server.expect(requestTo("https://example.test/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .header("retry-after", "60").body("{\"error\":{\"code\":\"rate_limit_exceeded\"}}"));
        assertTrue(client.summarizePlaces(targets(0, 6)).isEmpty());
        assertTrue(client.summarizePlaces(targets(10, 3)).isEmpty());
        server.verify();
    }

    @Test
    void rollingOutputBudgetBlocksUntilWindowExpires() throws Exception {
        expectBatch(0, 3, 500);
        expectBatch(10, 3, 150);
        assertEquals(3, client.summarizePlaces(targets(0, 3)).size());
        assertTrue(client.summarizePlaces(targets(10, 3)).isEmpty());
        now.addAndGet(60_001);
        assertEquals(3, client.summarizePlaces(targets(10, 3)).size());
        server.verify();
    }

    @Test
    void truncatedJsonIsNotUsedAsSuccessfulSummary() {
        server.expect(requestTo("https://example.test/chat/completions"))
                .andRespond(withSuccess("""
                        {"usage":{"completion_tokens":600},"choices":[{"finish_reason":"length",
                         "message":{"content":"{incomplete"}}]}
                        """, MediaType.APPLICATION_JSON));
        assertTrue(client.summarizePlaces(targets(0, 3)).isEmpty());
        server.verify();
    }
}
