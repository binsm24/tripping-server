package com.tripping.trippingserver.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Component
public class RecommendationTimingFilter extends OncePerRequestFilter {
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/recommendations");
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String previous = MDC.get("recommendationTrace");
        String trace = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("recommendationTrace", trace);
        response.setHeader("X-Recommendation-Trace", trace);
        long started = System.nanoTime();
        try { chain.doFilter(request, response); }
        finally {
            LoggerFactory.getLogger(getClass()).info(
                    "[RecommendationTiming] trace={} stage=total path={} status={} elapsedMs={}",
                    trace, request.getRequestURI(), response.getStatus(), (System.nanoTime() - started) / 1_000_000);
            if (previous == null) MDC.remove("recommendationTrace"); else MDC.put("recommendationTrace", previous);
        }
    }
}
