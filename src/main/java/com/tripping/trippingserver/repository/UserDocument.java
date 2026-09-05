package com.tripping.trippingserver.repository;

import lombok.Builder;
import lombok.Getter;

@Getter
public class UserDocument {

    private String userId;
    private String kakaoId;
    private String nickname;
    private String email;
    private String profileImageUrl;
    private String createdAt;
    private String updatedAt;

    public UserDocument() {
    }

    @Builder
    public UserDocument(
            String userId,
            String kakaoId,
            String nickname,
            String email,
            String profileImageUrl,
            String createdAt,
            String updatedAt
    ) {
        this.userId = userId;
        this.kakaoId = kakaoId;
        this.nickname = nickname;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}