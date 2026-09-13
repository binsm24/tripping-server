package com.tripping.trippingserver.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import java.io.IOException;

public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        Object failure = request.getAttribute("jwt.failure");
        String message = "로그인이 필요합니다. 카카오 로그인 후 다시 이용해 주세요.";
        if ("expired".equals(failure)) {
            message = "로그인이 만료되었습니다. 다시 로그인해 주세요.";
        } else if ("invalid".equals(failure)) {
            message = "로그인 정보가 유효하지 않습니다. 다시 로그인해 주세요.";
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Bearer");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        // Only fixed server-owned messages are written here.
        response.getWriter().write("{\"status\":401,\"success\":false,\"message\":\"" + message + "\"}");
    }
}
