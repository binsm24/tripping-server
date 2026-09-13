package com.tripping.trippingserver.external.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripping.trippingserver.exception.BusinessException;
import com.tripping.trippingserver.exception.ErrorCode;
import com.tripping.trippingserver.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.time.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GeminiQuotaTests {
    private final MutableClock clock = new MutableClock();
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final GeminiApiClient client = new GeminiApiClient(
            new GeminiApiProperties("test", "https://example.test", "gemini-test"), new ObjectMapper(), builder, clock);
    private static final String URL = "https://example.test/v1beta/models/gemini-test:generateContent?key=test";
    private static final String DAILY = """
            {"error":{"code":429,"status":"RESOURCE_EXHAUSTED","details":[
              {"@type":"type.googleapis.com/google.rpc.QuotaFailure","violations":[
                {"quotaId":"GenerateRequestsPerDayPerProjectPerModel-FreeTier","quotaValue":"20"}]},
              {"@type":"type.googleapis.com/google.rpc.RetryInfo","retryDelay":"8s"}]}}
            """;
    private static final String SUCCESS = """
            {"candidates":[{"content":{"parts":[{"text":"ok"}]}}]}
            """;

    @Test
    void dailyQuotaReturns429MessageAndDoesNotRetryEvenAfterEightSeconds() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON).body(DAILY));
        BusinessException error = assertThrows(BusinessException.class, () -> client.generateContent("test"));
        assertEquals(ErrorCode.AI_DAILY_QUOTA_EXCEEDED, error.getErrorCode());
        assertTrue(error.getMessage().contains("9월 13일 16:00"));
        var response = new GlobalExceptionHandler().handleBusinessException(error);
        assertEquals(429, response.getStatusCode().value());
        assertEquals(429, response.getBody().getStatus());
        assertFalse(response.getBody().isSuccess());
        clock.now = clock.now.plusSeconds(9);
        assertThrows(BusinessException.class, () -> client.generateContent("test again"));
        server.verify(); // Exactly one external call, no retry and no repeat call while blocked.
    }

    @Test
    void allowsExplicitNewRequestAfterPacificMidnight() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body(DAILY));
        server.expect(requestTo(URL)).andRespond(withSuccess(SUCCESS, MediaType.APPLICATION_JSON));
        assertThrows(BusinessException.class, () -> client.generateContent("test"));
        clock.now = Instant.parse("2026-09-13T07:00:00Z");
        assertEquals("ok", client.generateContent("new request"));
        server.verify();
    }

    @Test
    void minuteQuotaIsNotClassifiedAsDailyQuota() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .body(DAILY.replace("PerDay", "PerMinute")));
        server.expect(requestTo(URL)).andRespond(withSuccess(SUCCESS, MediaType.APPLICATION_JSON));
        var error = assertThrows(BusinessException.class, () -> client.generateContent("test"));
        assertEquals(ErrorCode.EXTERNAL_API_ERROR, error.getErrorCode());
        assertEquals("ok", client.generateContent("new request"));
        server.verify();
    }

    @Test
    void malformedRateLimitBodyRetainsExistingHandling() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("not json"));
        var error = assertThrows(BusinessException.class, () -> client.generateContent("test"));
        assertEquals(ErrorCode.EXTERNAL_API_ERROR, error.getErrorCode());
        server.verify();
    }

    @Test
    void winterResetUsesKorean1700() {
        clock.now = Instant.parse("2026-12-13T02:00:00Z");
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body(DAILY));
        var error = assertThrows(BusinessException.class, () -> client.generateContent("test"));
        assertTrue(error.getMessage().contains("12월 13일 17:00"));
        server.verify();
    }

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-09-13T02:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
        @Override public Instant instant() { return now; }
    }
}
