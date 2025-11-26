package com.example.youth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 도메인 및 네트워크 관련 설정을 중앙 집중식으로 관리하는 설정 클래스
 * 모든 외부 API URL, 도메인, 엔드포인트를 한 곳에서 관리
 */
@Configuration
public class NetworkConfig {

    // ========== 공공데이터 API ==========
    @Value("${public-data.lh.rental-house-list.url}")
    private String lhRentalHouseListUrl;

    @Value("${public-data.lh.rental-notice.url}")
    private String lhRentalNoticeUrl;

    @Value("${youth-policy.url}")
    private String youthPolicyUrl;

    // ========== 카카오맵 API ==========
    @Value("${kakao.map.base-url:https://dapi.kakao.com}")
    private String kakaoMapBaseUrl;

    @Value("${kakao.map.rest-api-key}")
    private String kakaoMapRestApiKey;

    // ========== Gemini API ==========
    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1}")
    private String geminiApiBaseUrl;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String geminiApiModel;

    // ========== 외부 링크 도메인 ==========
    private static final String LH_APPLY_DOMAIN = "https://apply.lh.or.kr";
    private static final String MYHOME_DOMAIN = "https://www.myhome.go.kr";

    // ========== WebClient 빈 정의 ==========

    /**
     * 한국토지주택공사 공공임대주택 단지정보 조회용 WebClient
     */
    @Bean(name = "lhWebClient")
    public WebClient lhRentalHouseListWebClient() {
        return WebClient.builder()
                .baseUrl(lhRentalHouseListUrl)
                .build();
    }

    /**
     * 한국토지주택공사 분양임대공고문 조회용 WebClient
     */
    @Bean(name = "lhRentalNoticeWebClient")
    public WebClient lhRentalNoticeWebClient() {
        return WebClient.builder()
                .baseUrl(lhRentalNoticeUrl)
                .build();
    }

    /**
     * 청년정책 API용 WebClient (큰 응답 처리용)
     */
    @Bean(name = "youthPolicyWebClient")
    public WebClient youthPolicyWebClient() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .baseUrl(youthPolicyUrl)
                .exchangeStrategies(strategies)
                .build();
    }

    /**
     * 카카오맵 API용 WebClient
     */
    @Bean(name = "kakaoMapWebClient")
    public WebClient kakaoMapWebClient() {
        return WebClient.builder()
                .baseUrl(kakaoMapBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Gemini API용 WebClient
     */
    @Bean(name = "geminiWebClient")
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl(geminiApiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ========== Getter 메서드 ==========

    public String getLhRentalHouseListUrl() {
        return lhRentalHouseListUrl;
    }

    public String getLhRentalNoticeUrl() {
        return lhRentalNoticeUrl;
    }

    public String getYouthPolicyUrl() {
        return youthPolicyUrl;
    }

    public String getKakaoMapBaseUrl() {
        return kakaoMapBaseUrl;
    }

    public String getKakaoMapRestApiKey() {
        return kakaoMapRestApiKey;
    }

    public String getGeminiApiBaseUrl() {
        return geminiApiBaseUrl;
    }

    public String getGeminiApiModel() {
        return geminiApiModel;
    }

    public static String getLhApplyDomain() {
        return LH_APPLY_DOMAIN;
    }

    public static String getMyhomeDomain() {
        return MYHOME_DOMAIN;
    }

    // ========== 편의 메서드 ==========

    /**
     * LH 신청 링크 생성
     */
    public String buildLhApplyLink(String path) {
        return LH_APPLY_DOMAIN + (path != null ? path : "/");
    }

    /**
     * MyHome 링크 생성
     */
    public String buildMyhomeLink(String path) {
        return MYHOME_DOMAIN + (path != null ? path : "/");
    }

    /**
     * Gemini API 엔드포인트 생성
     */
    public String buildGeminiApiEndpoint() {
        return String.format("%s/models/%s:generateContent", geminiApiBaseUrl, geminiApiModel);
    }
}

