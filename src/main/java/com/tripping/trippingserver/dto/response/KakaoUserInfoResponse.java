package com.tripping.trippingserver.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoUserInfoResponse {

    private Long id;

    @JsonProperty("connected_at")
    private String connectedAt;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Getter
    @NoArgsConstructor
    public static class KakaoAccount {

        private String email;

        private Profile profile;

        @JsonProperty("email_needs_agreement")
        private Boolean emailNeedsAgreement;

        @JsonProperty("profile_nickname_needs_agreement")
        private Boolean profileNicknameNeedsAgreement;
    }

    @Getter
    @NoArgsConstructor
    public static class Profile {

        private String nickname;

        @JsonProperty("profile_image_url")
        private String profileImageUrl;

        @JsonProperty("thumbnail_image_url")
        private String thumbnailImageUrl;
    }

    public String getNickname() {
        if (kakaoAccount == null || kakaoAccount.profile == null) {
            return null;
        }

        return kakaoAccount.profile.nickname;
    }

    public String getEmail() {
        if (kakaoAccount == null) {
            return null;
        }

        return kakaoAccount.email;
    }

    public String getProfileImageUrl() {
        if (kakaoAccount == null || kakaoAccount.profile == null) {
            return null;
        }

        return kakaoAccount.profile.profileImageUrl;
    }
}