package com.tripping.trippingserver.controller;

import com.tripping.trippingserver.dto.request.KakaoLoginRequest;
import com.tripping.trippingserver.dto.response.ApiResponse;
import com.tripping.trippingserver.dto.response.KakaoLoginResponse;
import com.tripping.trippingserver.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(
        name = "Auth",
        description = "인증 및 로그인 API"
)
public class AuthController {

    private final AuthService authService;

    @PostMapping("/kakao")
    @Operation(
            summary = "카카오 로그인",
            description = """
                    카카오 Access Token을 이용하여 로그인합니다.
                    최초 로그인 시 회원가입을 함께 처리합니다.
                    현재는 Swagger 테스트용 임시 응답을 반환합니다.
                    """
    )
    public ResponseEntity<ApiResponse<KakaoLoginResponse>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request
    ) {
        KakaoLoginResponse data =
                authService.kakaoLogin(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        "login successful",
                        data
                )
        );
    }
}
