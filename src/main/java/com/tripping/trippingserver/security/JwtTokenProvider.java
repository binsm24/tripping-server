package com.tripping.trippingserver.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secret)
        );
        this.accessTokenExpiration = accessTokenExpiration;
    }

    /**
     * 사용자 ID를 subject로 포함한 Access Token을 생성합니다.
     */
    public String createAccessToken(String userId) {
        Date now = new Date();
        Date expiration = new Date(
                now.getTime() + accessTokenExpiration
        );

        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * JWT의 서명과 만료 시간을 검증합니다.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * JWT에서 사용자 ID를 추출합니다.
     */
    public String getUserId(String token) {
        return parseClaims(token).getSubject();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}