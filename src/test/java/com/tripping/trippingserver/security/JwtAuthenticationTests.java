package com.tripping.trippingserver.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationTests {
    private final String secret = Base64.getEncoder().encodeToString(new byte[32]);
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    private MockHttpServletResponse invoke(String token) throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/saved-courses");
        var response = new MockHttpServletResponse();
        if (token != null) request.addHeader("Authorization", "Bearer " + token);
        new JwtAuthenticationFilter(new JwtTokenProvider(secret, 60000)).doFilter(request, response, (req, res) -> {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                new ApiAuthenticationEntryPoint().commence(request, response, null);
            }
        });
        return response;
    }
    @Test void missingToken() throws Exception {
        var response = invoke(null);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("로그인이 필요"));
    }
    @Test void expiredToken() throws Exception {
        var response = invoke(new JwtTokenProvider(secret, -60000).createAccessToken("user-1"));
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("만료"));
    }
    @Test void invalidToken() throws Exception {
        var response = invoke("invalid-token");
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("유효하지"));
    }
    @Test void validToken() throws Exception {
        assertEquals(200, invoke(new JwtTokenProvider(secret, 60000).createAccessToken("user-1")).getStatus());
        assertEquals("user-1", SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
