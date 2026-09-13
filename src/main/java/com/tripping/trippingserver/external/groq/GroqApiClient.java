package com.tripping.trippingserver.external.groq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import java.util.function.LongSupplier;

@Component
public class GroqApiClient {
    private static final Logger log = LoggerFactory.getLogger(GroqApiClient.class);
    private static final long WINDOW_MS = 60_000;
    private final GroqApiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final LongSupplier clock;
    private final Deque<Reservation> reservations = new ArrayDeque<>();
    private final LinkedHashMap<CacheKey, Cached> cache = new LinkedHashMap<>(16, 0.75f, true);
    private long blockedUntil;

    @Autowired
    public GroqApiClient(GroqApiProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, defaultBuilder(), System::currentTimeMillis);
    }

    GroqApiClient(GroqApiProperties properties, ObjectMapper objectMapper,
                  RestClient.Builder builder, LongSupplier clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = builder.build();
        this.clock = clock;
    }

    private static RestClient.Builder defaultBuilder() {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(8));
        return RestClient.builder().requestFactory(factory);
    }

    public Map<String, String> summarizePlaces(List<SummaryTarget> targets) {
        if (targets == null || targets.isEmpty()) return Map.of();
        long started = System.nanoTime();
        Map<String, String> result = new HashMap<>();
        Map<String, SummaryTarget> pending = new LinkedHashMap<>();
        for (var target : targets) {
            if (target == null || target.placeId == null || target.placeId.isBlank()
                    || target.name == null || target.name.isBlank()
                    || target.sourceText == null || target.sourceText.isBlank()) continue;
            String cached = cached(target);
            if (cached != null) result.put(target.placeId, cached);
            else pending.putIfAbsent(target.placeId, target);
        }
        List<SummaryTarget> missing = new ArrayList<>(pending.values());
        try {
            for (int i = 0; i < missing.size(); i += 3) {
                // Do not turn a token limit into a minute-long user-facing queue.
                if (System.nanoTime() - started > Duration.ofSeconds(12).toNanos()) break;
                List<SummaryTarget> batch = missing.subList(i, Math.min(i + 3, missing.size()));
                int budget = Math.min(properties.getMaxOutputTokens(), 120 + 160 * batch.size());
                Reservation reservation = reserve(budget);
                if (reservation == null) {
                    log.warn("[Groq] trace={} fallback=output_budget_or_cooldown pending={}",
                            MDC.get("recommendationTrace"), missing.size() - i);
                    break;
                }
                result.putAll(requestBatch(batch, budget, reservation));
            }
            return result;
        } finally {
            log.info("[Groq] trace={} targets={} summaries={} fallback={} elapsedMs={}",
                    MDC.get("recommendationTrace"), targets.size(), result.size(),
                    targets.size() - result.size(), (System.nanoTime() - started) / 1_000_000);
        }
    }

    private Map<String, String> requestBatch(List<SummaryTarget> targets, int budget, Reservation reservation) {
        long started = System.nanoTime();
        try {
            String prompt = """
                    다음 장소 목록의 원문에 있는 정보만 사용하여 각 장소의 한국어 한줄평을 작성해라.
                    모든 placeId를 그대로 유지하고 각각 30~50자의 자연스러운 한 문장을 작성한다.
                    장소명을 단순 반복하거나 새로운 사실을 만들지 않는다.
                    사고 과정, <think>, 마크다운, 부가 설명 없이 JSON 객체 하나만 반환한다.
                    형식: {"summaries":[{"placeId":"입력 ID","summary":"한줄평"}]}
                    장소 목록:
                    """ + objectMapper.writeValueAsString(targets);
            String raw = restClient.post().uri(properties.getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("model", properties.getModel(),
                            "messages", List.of(Map.of("role", "user", "content", prompt)),
                            "temperature", 0.2, "max_tokens", budget,
                            "reasoning_effort", "none", "reasoning_format", "hidden"))
                    .retrieve().body(String.class);
            JsonNode response = objectMapper.readTree(raw);
            JsonNode used = response.path("usage").path("completion_tokens");
            if (used.isIntegralNumber() && used.asInt() >= 0) settle(reservation, used.asInt());
            JsonNode choice = response.path("choices").path(0);
            if ("length".equals(choice.path("finish_reason").asText())) {
                log.warn("[Groq] trace={} fallback=truncated_json budget={} targets={}",
                        MDC.get("recommendationTrace"), budget, targets.size());
                return Map.of();
            }
            String content = choice.path("message").path("content").asText("")
                    .replaceAll("(?s)<think>.*?</think>", "")
                    .replace("```json", "").replace("```", "").trim();
            JsonNode summaries = objectMapper.readTree(content).path("summaries");
            if (!summaries.isArray()) return Map.of();
            Map<String, SummaryTarget> requested = new HashMap<>();
            targets.forEach(t -> requested.put(t.placeId, t));
            Map<String, String> result = new HashMap<>();
            for (JsonNode item : summaries) {
                String id = item.path("placeId").asText("");
                JsonNode text = item.path("summary");
                if (!requested.containsKey(id) || !text.isTextual() || text.asText().isBlank()) continue;
                String summary = text.asText().trim();
                result.put(id, summary);
                remember(requested.get(id), summary);
            }
            log.info("[Groq] trace={} batch={} budget={} completionTokens={} received={}",
                    MDC.get("recommendationTrace"), targets.size(), budget,
                    used.isIntegralNumber() ? used.asInt() : "unknown", result.size());
            return result;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                long cooldown = WINDOW_MS;
                String retryAfter = e.getResponseHeaders() == null ? null : e.getResponseHeaders().getFirst("retry-after");
                try { if (retryAfter != null) cooldown = Math.max(cooldown, Long.parseLong(retryAfter) * 1000); }
                catch (NumberFormatException ignored) { /* keep conservative 60-second cooldown */ }
                synchronized (this) { blockedUntil = Math.max(blockedUntil, clock.getAsLong() + cooldown); }
            }
            log.warn("[Groq] trace={} status={} targets={} budget={} fallback=true (no immediate retry)",
                    MDC.get("recommendationTrace"), e.getStatusCode().value(), targets.size(), budget);
            return Map.of();
        } catch (Exception e) {
            log.warn("[Groq] trace={} failure={} fallback=true", MDC.get("recommendationTrace"), e.getClass().getSimpleName());
            return Map.of();
        } finally {
            log.info("[Groq] trace={} batchElapsedMs={}", MDC.get("recommendationTrace"),
                    (System.nanoTime() - started) / 1_000_000);
        }
    }

    private synchronized Reservation reserve(int budget) {
        long now = clock.getAsLong();
        reservations.removeIf(r -> r.until <= now);
        if (budget <= 0 || now < blockedUntil
                || reservations.stream().mapToInt(r -> r.tokens).sum() + budget > properties.getOutputTokensPerMinute()) return null;
        Reservation reservation = new Reservation(now + WINDOW_MS, budget);
        reservations.addLast(reservation);
        return reservation;
    }
    private synchronized void settle(Reservation reservation, int tokens) {
        reservation.tokens = tokens;
        reservation.until = clock.getAsLong() + WINDOW_MS;
    }
    private synchronized String cached(SummaryTarget target) {
        Cached value = cache.get(new CacheKey(target.placeId, target.name, target.sourceText));
        return value != null && value.until > clock.getAsLong() ? value.summary : null;
    }
    private synchronized void remember(SummaryTarget target, String summary) {
        cache.put(new CacheKey(target.placeId, target.name, target.sourceText), new Cached(summary, clock.getAsLong() + 3_600_000));
        while (cache.size() > 512) cache.remove(cache.keySet().iterator().next());
    }
    private record CacheKey(String id, String name, String source) {}
    private record Cached(String summary, long until) {}
    private static class Reservation {
        long until; int tokens;
        Reservation(long until, int tokens) { this.until = until; this.tokens = tokens; }
    }
    @Getter
    public static class SummaryTarget {
        private final String placeId;
        private final String name;
        private final String sourceText;
        public SummaryTarget(String placeId, String name, String sourceText) {
            this.placeId = placeId; this.name = name; this.sourceText = sourceText;
        }
    }
}
