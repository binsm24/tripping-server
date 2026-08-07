package com.tripping.trippingserver.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "카카오 로그인 요청")
public class KakaoLoginRequest {

    @NotBlank
    @Schema(
            description = "카카오에서 발급받은 Access Token",
            example = "test-kakao-access-token"
    )
    private String accessToken;
}
