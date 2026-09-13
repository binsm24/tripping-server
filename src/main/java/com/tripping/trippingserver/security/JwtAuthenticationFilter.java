package com.tripping.trippingserver.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            try {
                // Parse once so expiration cannot occur between validation and subject lookup.
                String userId = jwtTokenProvider.getUserId(token);
                if (userId == null || userId.isBlank()) {
                    request.setAttribute("jwt.failure", "invalid");
                } else {
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(userId, null,
                                    List.of(new SimpleGrantedAuthority("ROLE_USER"))));
                }
            } catch (io.jsonwebtoken.ExpiredJwtException exception) {
                SecurityContextHolder.clearContext();
                request.setAttribute("jwt.failure", "expired");
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException exception) {
                SecurityContextHolder.clearContext();
                request.setAttribute("jwt.failure", "invalid");
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization =
                request.getHeader("Authorization");

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            return null;
        }

        String token = authorization.substring(7).trim();

        return token.isBlank() ? null : token;
    }
}
