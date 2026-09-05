package com.tripping.trippingserver.external.kakao;

import com.tripping.trippingserver.dto.response.KakaoTokenResponse;
import com.tripping.trippingserver.dto.response.KakaoUserInfoResponse;
import com.tripping.trippingserver.exception.BusinessException;
import com.tripping.trippingserver.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class KakaoApiClient {

    private final KakaoApiProperties properties;

    private final RestClient restClient =
            RestClient.builder().build();

    public KakaoTokenResponse exchangeCodeForToken(
            String code
    ) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "카카오 인가 코드가 없습니다."
            );
        }

        MultiValueMap<String, String> formData =
                new LinkedMultiValueMap<>();

        formData.add(
                "grant_type",
                "authorization_code"
        );
        formData.add(
                "client_id",
                properties.getRestApiKey()
        );
        formData.add(
                "redirect_uri",
                properties.getRedirectUri()
        );
        formData.add("code", code);

        if (properties.getClientSecret() != null
                && !properties.getClientSecret().isBlank()) {
            formData.add(
                    "client_secret",
                    properties.getClientSecret()
            );
        }

        try {
            KakaoTokenResponse response = restClient
                    .post()
                    .uri(properties.getTokenUrl())
                    .contentType(
                            MediaType.APPLICATION_FORM_URLENCODED
                    )
                    .body(formData)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            if (response == null
                    || response.getAccessToken() == null
                    || response.getAccessToken().isBlank()) {
                throw new BusinessException(
                        ErrorCode.UNAUTHORIZED,
                        "카카오 Access Token을 발급받지 못했습니다."
                );
            }

            return response;

        } catch (BusinessException exception) {
            throw exception;

        } catch (RestClientException exception) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "카카오 토큰 발급에 실패했습니다."
            );
        }
    }

    public KakaoUserInfoResponse getUserInfo(
            String accessToken
    ) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "카카오 Access Token이 없습니다."
            );
        }

        try {
            KakaoUserInfoResponse response = restClient
                    .get()
                    .uri(properties.getUserInfoUrl())
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + accessToken
                    )
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);

            if (response == null
                    || response.getId() == null) {
                throw new BusinessException(
                        ErrorCode.UNAUTHORIZED,
                        "카카오 사용자 정보를 확인할 수 없습니다."
                );
            }

            return response;

        } catch (BusinessException exception) {
            throw exception;

        } catch (RestClientException exception) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "유효하지 않은 카카오 Access Token입니다."
            );
        }
    }
}