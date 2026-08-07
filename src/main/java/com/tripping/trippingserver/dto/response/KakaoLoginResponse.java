package com.tripping.trippingserver.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "카카오 로그인 응답")
public class KakaoLoginResponse {

    @Schema(
            description = "TripPing 사용자 ID",
            example = "1"
    )
    private Long userId;

    @Schema(
            description = "사용자 닉네임",
            example = "TripPing 사용자"
    )
    private String nickname;

    @Schema(
            description = "TripPing에서 사용할 인증 토큰",
            example = "test-jwt-token"
    )
    private String accessToken;

    @Schema(
            description = "신규 회원 여부",
            example = "true"
    )
    private boolean newUser;
}
