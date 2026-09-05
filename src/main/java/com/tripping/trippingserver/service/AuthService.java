package com.tripping.trippingserver.service;

import com.tripping.trippingserver.dto.request.KakaoLoginRequest;
import com.tripping.trippingserver.dto.response.KakaoLoginResponse;
import com.tripping.trippingserver.dto.response.KakaoTokenResponse;
import com.tripping.trippingserver.dto.response.KakaoUserInfoResponse;
import com.tripping.trippingserver.exception.BusinessException;
import com.tripping.trippingserver.exception.ErrorCode;
import com.tripping.trippingserver.external.kakao.KakaoApiClient;
import com.tripping.trippingserver.repository.UserDocument;
import com.tripping.trippingserver.repository.UserRepository;
import com.tripping.trippingserver.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoApiClient kakaoApiClient;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public KakaoLoginResponse kakaoLogin(
            KakaoLoginRequest request
    ) {
        KakaoTokenResponse kakaoToken =
                kakaoApiClient.exchangeCodeForToken(
                        request.getCode()
                );

        KakaoUserInfoResponse kakaoUser =
                kakaoApiClient.getUserInfo(
                        kakaoToken.getAccessToken()
                );

        if (kakaoUser.getId() == null) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "카카오 사용자 ID를 확인할 수 없습니다."
            );
        }

        String userId =
                String.valueOf(kakaoUser.getId());

        try {
            UserDocument user =
                    userRepository.findById(userId);

            boolean newUser = false;

            if (user == null) {
                user = createUserDocument(
                        userId,
                        kakaoUser
                );

                userRepository.save(user);
                newUser = true;
            }

            String tripPingJwt =
                    jwtTokenProvider.createAccessToken(userId);

            return KakaoLoginResponse.builder()
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .accessToken(tripPingJwt)
                    .newUser(newUser)
                    .build();

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "사용자 정보를 처리하는 중 오류가 발생했습니다."
            );

        } catch (ExecutionException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "사용자 정보를 처리하는 중 오류가 발생했습니다."
            );
        }
    }

    private UserDocument createUserDocument(
            String userId,
            KakaoUserInfoResponse kakaoUser
    ) {
        String nickname = kakaoUser.getNickname();

        if (nickname == null || nickname.isBlank()) {
            nickname = "TripPing 사용자";
        }

        String now = LocalDateTime.now().toString();

        return UserDocument.builder()
                .userId(userId)
                .kakaoId(userId)
                .nickname(nickname)
                .email(kakaoUser.getEmail())
                .profileImageUrl(
                        kakaoUser.getProfileImageUrl()
                )
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}