package com.tripping.trippingserver.service;

import com.tripping.trippingserver.dto.request.KakaoLoginRequest;
import com.tripping.trippingserver.dto.response.KakaoLoginResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public KakaoLoginResponse kakaoLogin(
            KakaoLoginRequest request
    ) {
        return KakaoLoginResponse.builder()
                .userId(1L)
                .nickname("TripPing 사용자")
                .accessToken("test-jwt-token")
                .newUser(true)
                .build();
    }
}
