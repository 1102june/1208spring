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

/**
 * WebClient 빈 설정
 * PublicDataApiService에서 사용하는 WebClient 빈들을 생성
 */
@Configuration
public class WebClientConfig {

    @Value("${public-data.lh.rental-house-list.url:https://data.myhome.go.kr:443/rentalHouseList}")
    private String lhRentalHouseListUrl;

    @Value("${public-data.lh.rental-notice.url:https://apis.data.go.kr/B552555}")
    private String lhRentalNoticeUrl;

    @Value("${youth-policy.url:https://www.youthcenter.go.kr/go/ythip/getPlcy}")
    private String youthPolicyUrl;

    /**
     * SSL 검증을 비활성화하는 HttpClient 생성 (개발 환경용)
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

    /**
     * LH 임대주택 단지정보 조회용 WebClient
     * 큰 응답 데이터를 처리하기 위해 버퍼 크기를 5MB로 설정
     */
    @Bean(name = "lhWebClient")
    public WebClient lhWebClient() {
        HttpClient httpClient = createInsecureHttpClient();
        
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(5 * 1024 * 1024)) // 5MB
                .build();
        
        return WebClient.builder()
                .baseUrl(lhRentalHouseListUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }

    /**
     * LH 분양임대공고문 조회용 WebClient
     * 큰 응답 데이터를 처리하기 위해 버퍼 크기를 5MB로 설정
     */
    @Bean(name = "lhRentalNoticeWebClient")
    public WebClient lhRentalNoticeWebClient() {
        HttpClient httpClient = createInsecureHttpClient();
        
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(5 * 1024 * 1024)) // 5MB
                .build();
        
        return WebClient.builder()
                .baseUrl(lhRentalNoticeUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }

    /**
     * 청년정책 조회용 WebClient
     * 큰 응답 데이터를 처리하기 위해 버퍼 크기를 10MB로 설정
     */
    @Bean(name = "youthPolicyWebClient")
    public WebClient youthPolicyWebClient() {
        HttpClient httpClient = createInsecureHttpClient();
        
        // 큰 응답 데이터를 처리하기 위한 ExchangeStrategies 설정
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
        
        return WebClient.builder()
                .baseUrl(youthPolicyUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }
}

