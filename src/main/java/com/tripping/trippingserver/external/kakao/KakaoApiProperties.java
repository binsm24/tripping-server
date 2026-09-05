package com.tripping.trippingserver.external.kakao;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kakao")
public class KakaoApiProperties {

    private String restApiKey;

    private String clientSecret;

    private String redirectUri;

    private String tokenUrl;

    private String userInfoUrl;
}