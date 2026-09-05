package com.tripping.trippingserver.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "카카오 인가 코드 로그인 요청")
public class KakaoLoginRequest {

    @NotBlank
    @Schema(
            description = "카카오 OAuth 로그인 후 발급받은 인가 코드",
            example = "authorization-code"
    )
    private String code;
}