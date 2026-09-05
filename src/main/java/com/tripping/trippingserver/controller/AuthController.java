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
                카카오 인가 코드를 이용하여 로그인합니다.
                백엔드가 카카오 Access Token으로 교환한 뒤
                카카오 사용자 정보를 조회합니다.
                로그인 성공 시 TripPing JWT를 발급합니다.
                최초 로그인 사용자는 Firestore에 저장합니다.
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
