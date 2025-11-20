package com.example.youth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PublicDataApiConfig {

    @Value("${public-data.lh.rental-house-list.url}")
    private String lhRentalHouseListUrl;

    @Value("${public-data.lh.rental-notice.url}")
    private String lhRentalNoticeUrl;

    @Value("${youth-policy.url}")
    private String youthPolicyUrl;

    @Bean(name = "lhWebClient")
    public WebClient lhWebClient() {
        return WebClient.builder()
                .baseUrl(lhRentalHouseListUrl)
                .build();
    }

    @Bean(name = "lhRentalNoticeWebClient")
    public WebClient lhRentalNoticeWebClient() {
        // 엔드포인트: https://apis.data.go.kr/B552555/lhLeaseNoticeInfo1
        // 실제 호출 시 /lhLeaseNoticeInfo1 경로를 추가해야 함
        return WebClient.builder()
                .baseUrl(lhRentalNoticeUrl)
                .build();
    }

    @Bean(name = "youthPolicyWebClient")
    public WebClient youthPolicyWebClient() {
        // 큰 응답을 처리하기 위해 버퍼 크기 증가 (10MB)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .baseUrl(youthPolicyUrl)
                .exchangeStrategies(strategies)
                .build();
    }
}

