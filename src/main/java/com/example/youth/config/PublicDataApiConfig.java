package com.example.youth.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

@Configuration
public class PublicDataApiConfig {

    @Value("${public-data.lh.rental-house-list.url}")
    private String lhRentalHouseListUrl;

    @Value("${public-data.lh.rental-notice.url}")
    private String lhRentalNoticeUrl;

    @Value("${youth-policy.url}")
    private String youthPolicyUrl;

    /**
     * SSL 검증을 비활성화하는 HttpClient 생성 (개발 환경용)
     * 주의: 프로덕션 환경에서는 사용하지 마세요!
     */
    private HttpClient createInsecureHttpClient() {
        try {
            SslContext sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            
            return HttpClient.create()
                    .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));
        } catch (SSLException e) {
            throw new RuntimeException("SSL 컨텍스트 생성 실패", e);
        }
    }

    @Bean(name = "lhWebClient")
    public WebClient lhWebClient() {
        // SSL 검증 비활성화 (개발 환경용)
        HttpClient httpClient = createInsecureHttpClient();
        
        return WebClient.builder()
                .baseUrl(lhRentalHouseListUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean(name = "lhRentalNoticeWebClient")
    public WebClient lhRentalNoticeWebClient() {
        // 엔드포인트: https://apis.data.go.kr/B552555/lhLeaseNoticeInfo1
        // 실제 호출 시 /lhLeaseNoticeInfo1 경로를 추가해야 함
        // SSL 검증 비활성화 (개발 환경용)
        HttpClient httpClient = createInsecureHttpClient();
        
        return WebClient.builder()
                .baseUrl(lhRentalNoticeUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean(name = "youthPolicyWebClient")
    public WebClient youthPolicyWebClient() {
        // 큰 응답을 처리하기 위해 버퍼 크기 증가 (10MB)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        // SSL 검증 비활성화 (개발 환경용)
        HttpClient httpClient = createInsecureHttpClient();

        return WebClient.builder()
                .baseUrl(youthPolicyUrl)
                .exchangeStrategies(strategies)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}

